package com.yit.deploy.core.info;

import com.yit.deploy.core.collections.ReverseList;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.inherits.Inherits;
import com.yit.deploy.core.model.Environment;
import com.yit.deploy.core.model.Environments;
import com.yit.deploy.core.model.PlaybookParameterSpec;
import com.yit.deploy.core.variables.SimpleVariables;

import java.util.*;
import java.util.function.Function;

public class PlaybookInfo {
    private final String name;
    private String description;
    private final List<String> parents = new ArrayList<>();
    private final Map<String, PlaybookParameterSpec> parameterSpecs = new HashMap<>();
    private String activeInEnv;
    private final SimpleVariables vars = new SimpleVariables();
    private final Map<String, PlayInfo> plays = new HashMap<>();
    private final Map<String, PlaybookSceneInfo> scenes = new HashMap<>();
    private final transient List<PlaybookHookInfo> hooks = new ArrayList<>();
    /**
     * resource operators defined for plays
     */
    private Map<String, ResourceOperatorInfo> resourceOperators = new HashMap<>();

    public PlaybookInfo(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getParents() {
        return parents;
    }

    public Map<String, PlaybookParameterSpec> getParameterSpecs() {
        return parameterSpecs;
    }

    public String getActiveInEnv() {
        return activeInEnv;
    }

    public void setActiveInEnv(String activeInEnv) {
        this.activeInEnv = activeInEnv;
    }

    public SimpleVariables getVars() {
        return vars;
    }

    public Map<String, PlayInfo> getPlays() {
        return plays;
    }

    public Map<String, PlaybookSceneInfo> getScenes() {
        return scenes;
    }

    public List<PlaybookHookInfo> getHooks() {
        return hooks;
    }

    public Map<String, ResourceOperatorInfo> getResourceOperators() {
        return resourceOperators;
    }

    public static PlayInfo findSuperPlay(PlayInfo play, List<PlaybookInfo> playbookAscending) {
        String playName = play.getName();
        boolean found = false;
        for (PlaybookInfo pb : playbookAscending) {
            PlayInfo pi = pb.getPlays().get(playName);
            // p belongs to pb
            if (pi == play) {
                if (found) {
                    throw new IllegalStateException("duplicated play found: " + playName);
                }
                found = true;
            } else if (found && pi != null) {
                return pi;
            }
        }
        return null;
    }

    public static PlayInfo findPlay(String playName, List<PlaybookInfo> playbookAscending) {
        for (PlaybookInfo pb : playbookAscending) {
            PlayInfo pi = pb.getPlays().get(playName);
            if (pi != null) {
                return pi;
            }
        }
        return null;
    }

    private boolean matchParameters(Map<String, ?> parameters) {
        for (PlaybookParameterSpec spec : parameterSpecs.values()) {
            if (!spec.match(parameters)) {
                return false;
            }
        }
        return true;
    }

    public List<String> descending(Map<String, ?> parameters, Map<String, PlaybookInfo> infoMap) {
        Function<String, Iterator<String>> parentsAccessor = e -> Lambda.filter(
            infoMap.get(e).parents.iterator(),
            p -> infoMap.get(p).matchParameters(parameters)
        );

        return Inherits.descending(name, parentsAccessor);
    }

    public List<String> descending(Map<String, PlaybookInfo> infoMap) {
        return descending(parameterSpecs, infoMap);
    }

    public List<String> ascending(Map<String, PlaybookInfo> infoMap) {
        return new ReverseList<>(descending(infoMap));
    }

    public Environment getFinalActiveInEnv(Map<String, PlaybookInfo> playbooks, Environments envs) {
        for (String n : ascending(playbooks)) {
            String e = playbooks.get(n).getActiveInEnv();
            if (e != null) {
                return envs.get(e);
            }
        }
        return envs.getRootEnv();
    }
}
