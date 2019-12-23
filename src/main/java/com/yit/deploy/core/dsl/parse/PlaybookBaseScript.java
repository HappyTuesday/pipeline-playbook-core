package com.yit.deploy.core.dsl.parse;

import com.yit.deploy.core.config.DeployConfig;
import com.yit.deploy.core.dsl.BaseScript;
import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.PlayExecutionContext;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.*;
import com.yit.deploy.core.model.*;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.support.UserParameterValueTypesSupport;
import com.yit.deploy.core.support.VariableValueTypesSupport;
import com.yit.deploy.core.variables.LayeredVariables;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.io.Serializable;
import java.util.*;

public abstract class PlaybookBaseScript extends BaseScript implements VariableValueTypesSupport, UserParameterValueTypesSupport {

    private DeployConfig deployConfig;

    private Environments envs;
    private LayeredVariables envVars;
    private PlaybookInfo playbook;
    private Map<String, PlaybookInfo> playbooks;
    private LayeredVariables playbookVars;

    /**
     * variable provided to access setup/teardown hook
     */
    public final PlaybookHookDslContext hook = new PlaybookHookDslContext();

    /**
     * the hook that this playbook script will be defining.
     * it will be initialized at the first time when you accessing hook.setup / hook.teardown function.
     * and also at this time, this object gets into playbook.hooks list.
     */
    private PlaybookHookInfo definedHook = null;

    public void parse(String playbookName, Map<String, PlaybookInfo> playbooks, DeployConfig deployConfig, Environments envs) {
        if (playbooks.containsKey(playbookName)) {
            throw new IllegalArgumentException("playbook " + playbookName + " has already been parsed");
        }

        this.deployConfig = deployConfig;
        this.envs = envs;
        this.envVars = new LayeredVariables();
        this.playbookVars = new LayeredVariables();
        this.playbook = new PlaybookInfo(playbookName);
        this.playbooks = playbooks;
        playbooks.put(playbookName, this.playbook);

        resolveVars(this.envVars, this.playbookVars);
        setWritableVars(this.playbook.getVars());

        refreshPlaybookVars();
        refreshEnvVars();

        run();
    }

    private void refreshPlaybookVars() {
        playbookVars.clearLayers();
        for (String p : playbook.descending(playbooks)) {
            playbookVars.layer(playbooks.get(p).getVars());
        }
    }

    private void refreshEnvVars() {
        envVars.clearLayers();
        envVars.layer(playbook.getFinalActiveInEnv(playbooks, envs).getVars());
    }

    public void desc(String description) {
        playbook.setDescription(description);
    }

    public void activeInEnv(String envName) {
        if (playbook.getActiveInEnv().equals(envName)) {
            return;
        }
        if (!envs.contains(envName)) {
            throw new IllegalConfigException("invalid environment " + envName);
        }
        playbook.setActiveInEnv(envName);

        refreshEnvVars();
    }

    public void inherits(String playbook) {
        if (!playbooks.containsKey(playbook)) {
            deployConfig.getPlaybookScript(playbook).parse(playbook, playbooks, deployConfig, envs);
        }

        if (this.playbook.getParents().contains(playbook)) {
            throw new IllegalConfigException("parent " + playbook + " is already inherited by " + this.playbook.getName());
        }

        this.playbook.getParents().add(playbook);

        refreshPlaybookVars();

        if (this.playbook.getActiveInEnv() == null) {
            refreshEnvVars(); // activeInEnv maybe set by its parent
        }
    }

    /**
     * indicate that only when the given param's value in the given value list, this playbook will be activated
     * this instruct should be put to the first code line of this script
     * @param param param name
     * @param containedIn param target values
     */
    public void activeIf(String param, Object... containedIn) {
        if (!this.playbook.getParents().isEmpty() || !playbook.getPlays().isEmpty()) {
            throw new IllegalConfigException("activeWhen must be put before includes and play");
        }

        this.playbook.getParameterSpecs().put(param, new PlaybookParameterSpec(param, Arrays.asList(containedIn)));
        refreshPlaybookVars(); // parents is defined on parameters
        refreshEnvVars();
    }

    /**
     * indicate that only when the given param is set, this playbook will be activated
     * this instruct should be put to the first code line of this script
     * @param param param name
     */
    public void activeIfParamSet(String param) {
        if (!playbook.getParents().isEmpty() || !playbook.getPlays().isEmpty()) {
            throw new IllegalConfigException("activeWhen must be put before includes and play");
        }

        playbook.getParameterSpecs().put(param, new PlaybookParameterSpec(param, null));
        refreshPlaybookVars(); // parents is defined on parameters
        refreshEnvVars();
    }

    public PlaybookSceneContext scene(String name, String ... plays) {
        return new PlaybookSceneContext(playbook, name, Arrays.asList(plays));
    }

    public PlaybookSceneContext defaultScene(String ... plays) {
        return new PlaybookSceneContext(playbook, Playbook.DEFAULT_SCENE_NAME, Arrays.asList(plays));
    }

    private PlayContext createPlay(String name, boolean inheritsSuper, @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = PlayContext.class) Closure closure) {
        if (Play.INHERITS_SUPER.equals(name)) {
            throw new IllegalConfigException("name " + name + " is a reserved as a play name");
        }
        List<PlaybookInfo> playbookAscending = Lambda.map(playbook.ascending(playbooks), playbooks::get);
        PlayContext context = new PlayContext(getUnderlineVars(), playbookAscending, playbook, name);
        if (inheritsSuper) {
            context.inheritsSuper();
        }
        if (closure != null) {
            Closures.delegateOnly(context, closure);
        }
        return context;
    }

    public PlayContext play(String name) {
        return play(name, null);
    }

    public PlayContext play(String name, @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = PlayContext.class) Closure closure) {
        return createPlay(name, false, closure);
    }

    public PlayContext extendsPlay(String name) {
        return extendsPlay(name, null);
    }

    public PlayContext extendsPlay(String name, @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = PlayContext.class) Closure closure) {
       return createPlay(name, true, closure);
    }

    public void extendsPlay(List<String> names, @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = PlayContext.class) Closure closure) {
        for (String name : names) {
            extendsPlay(name, closure);
        }
    }

    /**
     * define an acquire resource method
     * @param resourceKey
     */
    public void acquireResource(String resourceKey, @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = PlayExecutionContext.class) Closure closure) {
        ResourceOperatorInfo op = playbook.getResourceOperators().computeIfAbsent(resourceKey, x -> new ResourceOperatorInfo());
        if (op.getAcquire() != null) {
            throw new IllegalStateException("acquire method for resource " + resourceKey + " in playbook is already defined");
        }
        op.setAcquire(new ClosureWrapper<>((Closure<?>) closure));
    }

    /**
     * define a release resource method
     * @param resourceKey
     */
    public void releaseResource(String resourceKey, @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = PlayExecutionContext.class) Closure closure) {
        ResourceOperatorInfo op = playbook.getResourceOperators().computeIfAbsent(resourceKey, x -> new ResourceOperatorInfo());
        if (op.getRelease() != null) {
            throw new IllegalStateException("release method for resource " + resourceKey + " in playbook is already defined");
        }
        op.setRelease(new ClosureWrapper<>((Closure<?>) closure));
    }

    public class PlaybookHookDslContext implements Serializable {
        public void setup(@DelegatesTo(value = JobExecutionContext.class, strategy = Closure.DELEGATE_ONLY) Closure closure) {
            if (definedHook == null) {
                definedHook = new PlaybookHookInfo();
                playbook.getHooks().add(definedHook);
            }
            if (definedHook.getSetup() != null) {
                throw new IllegalArgumentException("setup hook is already defined for playbook " + playbook.getName());
            }
            definedHook.setSetup(new ClosureWrapper<>((Closure<?>) closure));
        }

        public void teardown(@DelegatesTo(value = JobExecutionContext.class, strategy = Closure.DELEGATE_ONLY) Closure closure) {
            if (definedHook == null) {
                definedHook = new PlaybookHookInfo();
                playbook.getHooks().add(definedHook);
            }
            if (definedHook.getTeardown() != null) {
                throw new IllegalArgumentException("teardown hook is already defined for playbook " + playbook.getName());
            }
            definedHook.setTeardown(new ClosureWrapper<>((Closure<?>) closure));
        }
    }
}