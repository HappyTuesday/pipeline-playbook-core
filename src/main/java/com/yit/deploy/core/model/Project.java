package com.yit.deploy.core.model;

import com.yit.deploy.core.collections.ReverseList;
import com.yit.deploy.core.dsl.evaluate.JobEvaluationContext;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.ProjectInfo;
import com.yit.deploy.core.info.ProjectInfoAccessor;
import com.yit.deploy.core.variables.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Project {

    public static final char VARIABLE_GROUP_SEPARATOR = '_';

    public static final String JOB_NAME_VARIABLE = "$PROJECT_JOB_NAME";
    public static final String PLAYBOOK_PARAMETERS_VARIABLE = "$PLAYBOOK_PARAMETERS";
    public static final String GROUP_NAME_VARIABLE = "$PROJECT_GROUP_NAME";
    public static final String SECTION_NAME_VARIABLE = "$PROJECT_SECTION_NAME";
    public static final String JOB_ORDER_VARIABLE = "$PROJECT_JOB_ORDER";
    public static final String CONTAINER_LABELS_VARIABLE = "$PROJECT_CONTAINER_LABELS";
    public static final String PROJECT_GIT_NAME_VARIABLE = "$PROJECT_GIT_NAME";
    public static final String GIT_REPOSITORY_URL_VARIABLE = "$GIT_REPOSITORY_URL";
    public static final String DEFAULT_BRANCH_VARIABLE = "$PROJECT_DEFAULT_BRANCH";
    public static final String DEPENDENCY_VARIABLE = "$PROJECT_DEPENDENCY";
    public static final String SCHEDULE_VARIABLE = "$PROJECT_SCHEDULE";
    public static final String TASKS_TO_SKIP_VARIABLE = "$PROJECT_TASKS_TO_SKIP";
    public static final String REQUIRED_TASKS_VARIABLE = "$REQUIRED_TASKS";

    private final String projectName;
    private final String variableGroup;
    private final String key;
    private final boolean abstracted;
    private final List<Project> descending;
    private final String activeInEnv;
    private final String description;
    private final SimpleVariables vars;
    private final List<ProjectOverride> overrides;
    private final String playbookName;
    private final List<ClosureWrapper<Boolean>> when;
    private final List<String> includedInEnv;
    private final List<String> includedOnlyInEnv;
    private final List<String> excludedInEnv;
    /**
     * env a (non-abstract) -> env b (non-abstract), means the project is shared from b to a,
     * if we want to lookup a project var in a, but project is not defined in a, so b will be searched instead
     */
    private final Map<String, String> sharing;

    private final ConcurrentHashMap<String, LayeredVariables> declaredVarsForEnvCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LayeredVariables> varsForEnvCache = new ConcurrentHashMap<>();

    public Project(String projectName, Map<String, ProjectInfo> infoMap, Environments envs, Projects projects) {
        if (projectName == null) {
            throw new IllegalConfigException("project name is not set");
        }
        ProjectInfo info = infoMap.get(projectName);
        if (info == null) {
            throw new IllegalConfigException("could not find project info " + projectName);
        }

        this.key = info.getKey();

        this.projectName = info.getProjectName();
        this.abstracted = info.isAbstracted() != null && info.isAbstracted();

        List<ProjectInfo> descending = info.descending(ProjectInfoAccessor.from(infoMap));
        this.descending = Lambda.map(descending, x -> x == info ? this : projects.get(x.getProjectName()));
        Iterable<ProjectInfo> ascending = new ReverseList<>(descending);

        this.activeInEnv = Lambda.cascade(ascending, ProjectInfo::getActiveInEnv);
        if (!this.abstracted && this.activeInEnv == null) {
            throw new IllegalConfigException("activeInEnv is not set in project " + this.projectName + " or its parents");
        }
        if (!envs.contains(this.activeInEnv)) {
            throw new IllegalConfigException("activeInEnv " + this.activeInEnv + " is invalid for project " + projectName);
        }
        this.description = info.getDescription();

        this.vars = info.getVars();
        this.overrides = info.getOverrides();

        this.playbookName = Lambda.cascade(ascending, ProjectInfo::getPlaybookName);
        if (!this.abstracted && this.playbookName == null) {
            throw new IllegalConfigException("playbook is not set in project " + this.projectName + " or its parents");
        }

        this.when = Lambda.cascade(ascending, ProjectInfo::getWhen);
        this.includedInEnv = info.getIncludedInEnv();
        this.includedOnlyInEnv = info.getIncludedOnlyInEnv();
        this.excludedInEnv = info.getExcludedInEnv();

        ClosureWrapper<String> variableGroupGenerator = Lambda.cascade(ascending, ProjectInfo::getVariableGroupGenerator);
        if (variableGroupGenerator != null) {
            Object o = variableGroupGenerator.call(this);
            if (o != null) {
                this.variableGroup = o.toString();
            } else {
                this.variableGroup = null;
            }
        } else {
            this.variableGroup = null;
        }

        this.sharing = new HashMap<>();

        List<Map<String, String>> inheritedSharing = new ArrayList<>(1);
        for (ProjectInfo p : descending) {
            if (p.getSharing() != null && !p.getSharing().isEmpty()) {
                inheritedSharing.add(p.getSharing());
            }
        }

        if (!inheritedSharing.isEmpty()) {
            for (Environment env : envs) {
                if (env.isAbstracted()) continue;

                String target = null;
                for (Map<String, String> m : inheritedSharing) {
                    for (Map.Entry<String, String> entry : m.entrySet()) {
                        if (env.isIncludedIn(entry.getKey())) {
                            target = entry.getValue();
                        }
                    }
                }

                if (target != null && !target.equals(env.getName())) {
                    Environment targetEnv = envs.get(target);
                    if (targetEnv.isAbstracted()) {
                        throw new IllegalConfigException("env " + target + " is not abstracted, " +
                            "only non-abstracted env can be shared.");
                    }
                    this.sharing.put(env.getName(), target);
                }
            }
        }
    }

    public LayeredVariables getVarsForEnv(Environment env) {
        return varsForEnvCache.computeIfAbsent(env.getName(), key -> getVarsForEnvWithoutCache(env));
    }

    private LayeredVariables getVarsForEnvWithoutCache(Environment env) {
        LayeredVariables layers = new LayeredVariables();
        for (Project p : descending) {
            layers.layer(p.getDeclaredVarsForEnv(env));
        }
        return layers;
    }

    public LayeredVariables getDeclaredVarsForEnv(Environment env) {
        return declaredVarsForEnvCache.computeIfAbsent(env.getName(), key -> getDeclaredVarsForEnvWithoutCache(env));
    }

    private LayeredVariables getDeclaredVarsForEnvWithoutCache(Environment env) {
        LayeredVariables layers = new LayeredVariables();

        if (!this.vars.isEmpty()) {
            layers.layer(this.vars);
        }

        if (this.overrides.isEmpty()) {
            return layers;
        }

        LinkedList<ProjectOverride> list = new LinkedList<>();
        for (ProjectOverride o : this.overrides) {
            if (o.getQuery().match(env)) {
                list.add(o);
            }
        }

        if (list.isEmpty()) {
            return layers;
        }

        for (Environment e : env.getDescending()) {
            for (Iterator<ProjectOverride> iter = list.iterator(); iter.hasNext();) {
                ProjectOverride o = iter.next();
                if (o.getQuery().match(e)) {
                    // avoid to be consumed by its children
                    iter.remove();
                    if (!o.getVars().isEmpty()) {
                        layers.layer(o.getVars());
                    }
                }
            }
        }

        return layers;
    }

    public boolean isAbstracted() {
        return abstracted;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getVariableGroup() {
        return variableGroup;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getSharing() {
        return sharing;
    }

    public String getKey() {
        return key;
    }

    public String getPlaybookName() {
        return playbookName;
    }

    public boolean isEnabledIn(JobEvaluationContext context) {
        Environment env = context.getJob().getEnv();
        if (!env.belongsTo(activeInEnv)) {
            return false;
        }

        boolean excluded = false;
        for (Project p : descending) {
            if (p.includedInEnv != null && env.isIncludedIn(p.includedInEnv)) {
                excluded = false;
            }
            if (p.includedOnlyInEnv != null) {
                if (env.isIncludedIn(p.includedOnlyInEnv)) {
                    excluded = false;
                } else {
                    return false;
                }
            }
            if (p.excludedInEnv != null && env.isIncludedIn(p.excludedInEnv)) {
                excluded = true;
            }
        }
        if (excluded) {
            return false;
        }

        if (when != null) {
            for (ClosureWrapper<Boolean> closure : when) {
                if (!closure.withDelegateOnly(context)) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean belongsTo(String parentName) {
        return Lambda.any(descending, p -> p.projectName.equals(parentName));
    }
}