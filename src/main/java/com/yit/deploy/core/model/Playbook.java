package com.yit.deploy.core.model;

import com.yit.deploy.core.collections.ReverseList;
import com.yit.deploy.core.dsl.evaluate.JobEvaluationContext;
import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.exceptions.ExitPlayBookException;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.info.*;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.function.Holder;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.variables.LayeredVariables;
import com.yit.deploy.core.variables.SimpleVariables;
import com.yit.deploy.core.variables.Variables;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Playbook {
    public static final String DEFAULT_SCENE_NAME = "default";

    private final String name;
    private final String description;
    private final Map<String, PlaybookParameterSpec> parameterSpecs;
    private final String activeInEnv;
    private final Map<String, Play> plays;
    /**
     * all variables defined in this playbook, excluding its parents
     */
    private final transient LayeredVariables vars;
    private final Map<String, PlaybookScene> scenes;
    private final List<PlaybookHook> hooks;
    /**
     * resource operators defined for plays
     */
    private Map<String, ResourceOperator> resourceOperators;

    public Playbook(PlaybookInfo info, Map<String, Object> parameters, Map<String, PlaybookInfo> infoMap) {
        if (info.getName() == null) {
            throw new IllegalConfigException("playbook name is not set");
        }
        this.name = info.getName();
        this.description = info.getDescription();

        List<PlaybookInfo> infoDescending = Lambda.map(info.descending(parameters, infoMap), infoMap::get);
        List<PlaybookInfo> infoAscending = new ReverseList<>(infoDescending);

        this.parameterSpecs = new HashMap<>(parameters.size());
        for (PlaybookInfo p : infoDescending) {
            for (PlaybookParameterSpec spec : p.getParameterSpecs().values()) {
                this.parameterSpecs.compute(spec.name, (n, v) -> v == null ? spec : v.intersect(spec));
            }
        }

        this.activeInEnv = Lambda.cascade(Lambda.cascade(infoAscending, PlaybookInfo::getActiveInEnv), Environments.ROOT_ENVIRONMENT_NAME);

        Map<String, PlayInfo> playInfos = new HashMap<>();
        for (PlaybookInfo p : infoDescending) {
            // play defined in child playbook override the play with the same name defined in parents
            playInfos.putAll(p.getPlays());
        }
        this.plays = Lambda.mapValues(playInfos, pi -> new Play(pi, infoAscending));

        this.vars = new LayeredVariables();
        for (PlaybookInfo p : infoDescending) {
            if (!p.getVars().isEmpty()) {
                this.vars.layer(p.getVars());
            }
        }

        Map<String, PlaybookSceneInfo> sceneInfos = new HashMap<>(info.getScenes().size());
        for (PlaybookInfo p : infoDescending) {
            sceneInfos.putAll(p.getScenes());
        }
        this.scenes = Lambda.mapValues(sceneInfos, PlaybookScene::new);

        this.hooks = new ArrayList<>(info.getHooks().size());
        for (PlaybookInfo p : infoDescending) {
            for (PlaybookHookInfo item : p.getHooks()) {
                this.hooks.add(new PlaybookHook(item));
            }
        }

        this.resourceOperators = new HashMap<>();
        for (PlaybookInfo p : infoDescending) {
            for (Map.Entry<String, ResourceOperatorInfo> entry : p.getResourceOperators().entrySet()) {
                this.resourceOperators.put(entry.getKey(), new ResourceOperator(entry.getValue()));
            }
        }
    }

    public boolean matchParameters(Map<String, Object> parameters) {
        for (PlaybookParameterSpec spec : parameterSpecs.values()) {
            if (!spec.match(parameters)) {
                return false;
            }
        }
        return true;
    }

    public PlaybookScene getDefaultScene() {
        return getScene(DEFAULT_SCENE_NAME);
    }

    public PlaybookScene getScene(String sceneName) {
        return scenes.get(sceneName);
    }

    public Map<String, Play> getPlays() {
        return plays;
    }

    public void execute(DeploySpec spec, JobExecutionContext context) {

        Map<String, Variables> hostWritableTable = new ConcurrentHashMap<>(spec.servers.size() + 1);
        Function<Host, Variables> writable = host -> hostWritableTable.computeIfAbsent(host.getName(), k -> new SimpleVariables());

        int i;
        Throwable hookException = null;
        final Holder<Throwable> executionException = new Holder<>();

        for (i = 0; i < hooks.size(); i++) {
            if (hooks.get(i).getSetup() != null) {
                try {
                    hooks.get(i).getSetup().delegateOnly(context);
                } catch (Exception t) {
                    hookException = t;
                    break;
                }
            }
        }

        if (hookException == null) {
            List<Play> plays = getPlays(spec.plays);
            boolean containsAlwaysRun = Lambda.any(plays, Play::isAlwaysRun);
            ResourceOperator.using(
                plays, resourceOperators, context,
                Play::getResourcesRequired,
                p -> {
                    if (executionException.data == null) {
                        try {
                            p.execute(spec, context, writable);
                        } catch (Exception t) {
                            executionException.data = t;
                            if (containsAlwaysRun) {
                                context.getScript().warn("execution play %s failed: %s", p.getName(), t.getMessage());
                            }
                        }
                    } else if (p.isAlwaysRun()) {
                        try {
                            context.getScript().info("We are in always run mode!!!");
                            p.execute(spec, context, writable);
                        } catch (Exception t) {
                            context.getScript().warn("execute play %s failed: %s", p.getName(), t);
                        }
                    }
                });
        }

        for (i--; i >= 0; i--) {
            if (hooks.get(i).getTeardown() != null) {
                try {
                    hooks.get(i).getTeardown().delegateOnly(context);
                } catch (Exception t) {
                    context.getScript().warn("execute teardown hook of playbook %s failed: %s", name, t.getMessage());
                }
            }
        }

        Throwable finalException = hookException != null ? hookException : executionException.data;

        if (finalException != null) {
            if (finalException instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            if (finalException instanceof ExitPlayBookException) {
                context.getScript().info("Exit Playbook: " + finalException.getMessage());
                return;
            }

            if (finalException instanceof RuntimeException) {
                throw (RuntimeException) finalException;
            }

            throw new RuntimeException(finalException.getMessage(), finalException);
        }
    }

    public boolean isEnabledIn(Environment env) {
        return env.belongsTo(activeInEnv);
    }

    public List<String> getEnabledPlays(JobEvaluationContext context, List<String> playNames) {
        return Lambda.map(filterPlays(context, playNames), Play::getName);
    }

    public List<String> getAllEnabledPlays(JobEvaluationContext context) {
        return Lambda.map(Lambda.findAll(plays.values(), p -> p.isEnabledIn(context)), Play::getName);
    }

    public List<ProjectParameter> getUserParameters(JobEvaluationContext context, List<String> playNames) {
        List<ProjectParameter> ps = ProjectParameter.getParameters(context.getUnderlineVars(), context);
        for (Play p : getPlays(playNames)) {
            ProjectParameter.mergeList(ps, p.getParameters(context));
        }
        ProjectParameter.sortList(ps);
        return ps;
    }

    public List<String> getAllTaskTags(Job job, List<String> playNames) {
        Set<String> tags = new HashSet<>();
        for (Play p : getPlays(playNames)) {
            tags.addAll(p.getEnabledTags(job));
        }
        return new ArrayList<>(tags);
    }

    public List<String> getAllServers(JobEvaluationContext context, List<String> playNames) {
        List<String> servers = new ArrayList<>();
        for (Play p : getPlays(playNames)) {
            servers.addAll(Lambda.map(p.getTargetHosts(context), Host::getName));
        }
        servers = Lambda.unique(servers);
        servers.remove("localhost");
        return servers;
    }

    private List<Play> filterPlays(JobEvaluationContext context, List<String> playNames) {
        List<Play> list = new ArrayList<>(playNames.size());
        for (String playName : playNames) {
            Play play = getPlay(playName);
            if (play.isEnabledIn(context)) {
                list.add(play);
            }
        }
        return list;
    }

    private List<Play> getPlays(List<String> playNames) {
        return Lambda.map(playNames, this::getPlay);
    }

    private Play getPlay(String playName) {
        Play play = plays.get(playName);
        if (play == null) {
            throw new IllegalArgumentException("could not find play " + playName + " in playbook " + name);
        }
        return play;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, PlaybookParameterSpec> getParameterSpecs() {
        return parameterSpecs;
    }

    public LayeredVariables getVars() {
        return vars;
    }
}