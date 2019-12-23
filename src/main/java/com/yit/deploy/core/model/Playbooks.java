package com.yit.deploy.core.model;

import com.yit.deploy.core.config.DeployConfig;
import com.yit.deploy.core.dsl.parse.PlaybookBaseScript;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.info.PlaybookInfo;
import com.yit.deploy.core.inherits.Inherits;

import java.util.*;
import java.util.function.Consumer;

public class Playbooks {

    private final transient Map<String, PlaybookInfo> infoMap;
    private final LinkedHashMap<String, PlaybookGroup> map;

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public Playbooks(Map<String, PlaybookInfo> infoMap) {
        this.infoMap = infoMap;
        this.map = new LinkedHashMap<>(infoMap.size());
        for (PlaybookInfo info : this.infoMap.values()) {
            this.map.put(info.getName(), new PlaybookGroup(info, this.infoMap));
        }
    }

    public boolean isValidPlaybook(String playbookName) {
        return infoMap.containsKey(playbookName);
    }

    public Playbook getOrCreate(String name, Map<String, Object> parameters) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }

        PlaybookGroup group = map.get(name);
        if (group == null) {
            throw new IllegalConfigException("invalid playbook name " + name);
        }

        return group.getOrCreate(infoMap.get(name), parameters, infoMap);
    }

    /**
     * represent a set of playbooks with the same name but has different parameter specs
     */
    private static class PlaybookGroup {
        private final Set<String> validParameters;
        private final List<Playbook> list;

        private PlaybookGroup(PlaybookInfo info, Map<String, PlaybookInfo> infoMap) {
            this.validParameters = new HashSet<>();
            Consumer<String> visitor = p -> this.validParameters.addAll(infoMap.get(p).getParameterSpecs().keySet());
            Inherits.descending(info.getName(), p -> infoMap.get(p).getParents().iterator(), visitor);
            this.list = new ArrayList<>(this.validParameters.size());
        }

        private Playbook getOrCreate(PlaybookInfo info, Map<String, Object> parameters, Map<String, PlaybookInfo> infoMap) {
            for (Playbook p : list) {
                boolean mismatch = false;
                // find the first playbook which matches all the parameters provided
                for (String name : parameters.keySet()) {
                    if (validParameters.contains(name)) {
                        // if a valid parameter is not included in this playbook, it must not be the one we want
                        if (!p.getParameterSpecs().containsKey(name) || !p.matchParameters(parameters)) {
                            mismatch = true;
                            break;
                        }
                    }
                }
                if (!mismatch) {
                    return p;
                }
            }

            Playbook playbook = new Playbook(info, parameters, infoMap);
            list.add(playbook);
            return playbook;
        }
    }

    public static Map<String, PlaybookInfo> load(DeployConfig deployConfig, Environments envs) {
        DeployConfig.Folder root = deployConfig.getPlaybooksFolder();
        List<DeployConfig.File> files = root.listScriptsRecursive();
        Map<String, PlaybookInfo> playbooks = new HashMap<>(files.size());
        for (DeployConfig.File file : files) {
            String name = file.getPlaybookName();
            if (!playbooks.containsKey(name)) {
                file.getScript(PlaybookBaseScript.class).parse(name, playbooks, deployConfig, envs);
            }
        }
        return playbooks;
    }
}
