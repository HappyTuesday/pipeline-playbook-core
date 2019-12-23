package com.yit.deploy.core.support;

import com.yit.deploy.core.model.Build;

import java.util.function.Function;

public interface ConfigSupport {

    Build getBuild();

    default String getProjectConfig(String namespace, String key) {
        return getProjectConfig(namespace, key, getBuild().getJob().getProjectName());
    }

    default String getProjectConfig(String namespace, String key, String projectName) {
        return getProjectConfig(namespace, key, projectName, getBuild().getJob().getEnvName());
    }

    default String getProjectConfig(String namespace, String key, String projectName, String envName) {
        return getBuild().getService().getConfigValue(envName, projectName, namespace, key, getDefaultLocker());
    }

    default String getEnvConfig(String namespace, String key) {
        return getEnvConfig(namespace, key, getBuild().getJob().getEnvName());
    }

    default String getEnvConfig(String namespace, String key, String envName) {
        return getBuild().getService().getConfigValue(envName, null, namespace, key, getDefaultLocker());
    }

    default String getGlobalConfig(String namespace, String key) {
        return getBuild().getService().getConfigValue(null, null, namespace, key, getDefaultLocker());
    }

    default void updateProjectConfig(String namespace, String key, Function<String, String> f) {
        updateProjectConfig(namespace, key, getBuild().getJob().getProjectName(), f);
    }

    default void updateProjectConfig(String namespace, String key, String projectName, Function<String, String> f) {
        updateProjectConfig(namespace, key, projectName, getBuild().getJob().getEnvName(), f);
    }

    default void updateProjectConfig(String namespace, String key, String projectName, String envName, Function<String, String> f) {
        getBuild().getService().updateConfigValue(envName, projectName, namespace, key, getDefaultLocker(), f);
    }

    default void updateEnvConfig(String namespace, String key, Function<String, String> f) {
        updateEnvConfig(namespace, key, getBuild().getJob().getEnvName(), f);
    }

    default void updateEnvConfig(String namespace, String key, String envName, Function<String, String> f) {
        getBuild().getService().updateConfigValue(envName, null, namespace, key, getDefaultLocker(), f);
    }

    default void updateGlobalConfig(String namespace, String key, Function<String, String> f) {
        getBuild().getService().updateConfigValue(null, null, namespace, key, getDefaultLocker(), f);
    }

    default String getDefaultLocker() {
        return String.format("%s[build:%d,thread:%d]", getBuild().getJob().getJobName(),
            getBuild().getId(), Thread.currentThread().getId());
    }
}
