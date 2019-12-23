package com.yit.deploy.core.dsl.parse;

import javax.annotation.Nonnull;

public interface KeyedProjectDSL extends ProjectDSL {

    String getKey();

    default KeyedProjectDSL projectName(@Nonnull String projectName) {
        getProject().setProjectName(projectName);
        return this;
    }
}