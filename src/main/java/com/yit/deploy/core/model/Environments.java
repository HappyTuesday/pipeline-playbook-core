package com.yit.deploy.core.model;

import com.yit.deploy.core.config.DeployConfig;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.function.Holder;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.EnvironmentInfo;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class Environments implements Iterable<Environment> {

    public static final String ROOT_ENVIRONMENT_NAME = "shared.defaults";

    private final LinkedHashMap<String, Environment> map;

    public Environments(Map<String, EnvironmentInfo> infoMap) {
        this.map = new LinkedHashMap<>(infoMap.size());

        Holder<Consumer<String>> convert = new Holder<>();
        convert.data = name -> {
            for (String p : infoMap.get(name).getParents()) {
                if (!this.map.containsKey(p)) {
                    convert.data.accept(p);
                }
            }
            this.map.put(name, new Environment(name, infoMap, this));
        };

        for (String key : infoMap.keySet()) {
            if (!this.map.containsKey(key)) {
                convert.data.accept(key);
            }
        }
    }

    public Environment get(String name) {
        if (name == null) {
            return null;
        }

        Environment env = map.get(name);
        if (env == null) {
            throw new IllegalConfigException("could not find environment " + name);
        }
        return env;
    }

    /**
     * get the most base envs who matches the given key, the base of all envs who match
     * @param key env name / type / label
     * @return the most base env
     */
    public List<Environment> getsByKey(String key) {
        List<Environment> list = new LinkedList<>();
        for (Environment e : map.values()) {
            if (e.match(key)) {
                // all envs inheriting e will be replaced by e
                list.removeIf(x -> x.belongsTo(e));
                list.add(e);
            }
        }

        if (list.isEmpty()) {
            throw new IllegalConfigException("could not find environment by key " + key);
        }

        return list;
    }

    /**
     * get the most base env who matches the given key, the base of all envs who match
     * @param query env query string
     * @return the most base env
     */
    public Collection<Environment> query(String query) {
        return Lambda.asList(Lambda.filter(map.values().iterator(), e -> e.isIncludedIn(query)));
    }

    public boolean contains(String name) {
        return map.containsKey(name);
    }

    public boolean hasRootEnv() {
        return map.containsKey(ROOT_ENVIRONMENT_NAME);
    }

    public Environment getRootEnv() {
        return get(ROOT_ENVIRONMENT_NAME);
    }

    public int size() {
        return map.size();
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<Environment> iterator() {
        return map.values().iterator();
    }

    public static Map<String, EnvironmentInfo> load(DeployConfig deployConfig) {
        List<DeployConfig.File> files = deployConfig.getEnvsFolder().listScriptsRecursive();
        Map<String, EnvironmentInfo> envs = new HashMap<>(files.size());

        for (DeployConfig.File file : files) {
            String name = file.getEnvironmentName();
            if (!envs.containsKey(name)) {
                deployConfig.getEnvironmentScript(name).parse(name, deployConfig, envs);
            }
        }

        return envs;
    }

    public static String envName(@Nullable String name) {
        return name == null ? ROOT_ENVIRONMENT_NAME : name;
    }
}
