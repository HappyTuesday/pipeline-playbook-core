package com.yit.deploy.core.info;

import com.yit.deploy.core.exceptions.MissingProjectException;

import java.util.Map;
import java.util.Objects;

@FunctionalInterface
public interface ProjectInfoAccessor {
    ProjectInfo findProject(String projectName);

    default boolean exist(String projectName) {
        return findProject(projectName) != null;
    }

    default ProjectInfo getProject(String projectName) {
        ProjectInfo p = findProject(projectName);
        if (p == null) {
            throw new MissingProjectException(projectName);
        }
        return p;
    }

    default ProjectInfoAccessor with(Map<String, ProjectInfo> more) {
        return name -> {
            ProjectInfo p = more.get(name);
            if (p != null) {
                return p;
            }
            return this.findProject(name);
        };
    }

    default ProjectInfoAccessor with(ProjectInfo more) {
        return name -> Objects.equals(name, more.getProjectName()) ? more : this.findProject(name);
    }

    static ProjectInfoAccessor from(Map<String, ProjectInfo> map) {
        return map::get;
    }
}
