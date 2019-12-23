package com.yit.deploy.core.dsl.support;

import com.yit.deploy.core.exceptions.InvalidVariableDefinitionException;
import com.yit.deploy.core.exceptions.MissingVariableException;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.*;
import com.yit.deploy.core.variables.Variables;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.variables.resolvers.VariableResolving;
import com.yit.deploy.core.variables.variable.Variable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public interface ProjectEvaluationSupport extends VariableResolver {

    Environment getEnv();
    Project getProject();
    Variables getProjectVars();

    DeployModelTable getModelTable();

    ProjectEvaluationSupport withProject(Project project, Environment env);

    default ProjectEvaluationSupport withProject(String projectName, String envName) {
        return withProject(getModelTable().getProjects().get(projectName), getModelTable().getEnv(envName));
    }

    /**
     * @deprecated replaced by #{@link #getDeployConfigCommitHash()}
     */
    @Deprecated
    default String getDEPLOY_CONFIG_COMMIT_HASH() {
        return getDeployConfigCommitHash();
    }

    default String getDeployConfigCommitHash() {
        return Lambda.cascade(getModelTable().getDeployConfig().getCommitHash(), "ut");
    }

    /**
     * @deprecated replaced by #{@link #getProject()}
     */
    @Deprecated
    default Project getCurrentProject() {
        return getProject();
    }

    /**
     * @deprecated replaced by #{@link #getProject()}
     */
    @Deprecated
    default Project getTargetProject() {
        return getProject();
    }

    /**
     * @deprecated replaced by #{@link #getProject()}
     */
    @Deprecated
    default Project getCURRENT_PROJECT() {
        return getProject();
    }

    default Job findJob(String projectName) {
        return getModelTable().findJob(projectName, getEnv().getName());
    }

    default Job getJob(String projectName) {
        return getModelTable().getJob(projectName, getEnv().getName());
    }

    default Job findJob(String projectName, String envName) {
        return getModelTable().findJob(projectName, envName);
    }

    default Job getJob(String projectName, String envName) {
        return getModelTable().getJob(projectName, envName);
    }

    default Job findJobByName(String jobName) {
        return getModelTable().findJobByName(jobName);
    }

    default Job getJobByName(String jobName) {
        return getModelTable().getJobByName(jobName);
    }

    default Project getProject(String projectName) {
        return getModelTable().getProjects().get(projectName);
    }

    default Object getProjectVar(String variableName, String projectName) {
        return getProjectVar(variableName, projectName, getEnv().getName());
    }

    default Object getProjectVar(String variableName, String projectName, String envName) {
        return withProject(projectName, envName).getVariable(variableName);
    }

    default Object getProjectVarOrDefault(String variableName, String projectName) {
        return getProjectVarOrDefault(variableName, projectName, getEnv().getName());
    }

    default Object getProjectVarOrDefault(String variableName, String projectName, String envName) {
        return withProject(projectName, envName).getVariableOrDefault(variableName);
    }

    default Object getVariable(String variableName, String projectName, String envName) {
        return withProject(projectName, Lambda.cascade(envName, getEnv().getName())).getVariable(variableName);
    }

    default Object getVariable(String variableName, String projectName) {
        return getVariable(variableName, projectName, getEnv().getName());
    }

    default Object getVariableOrDefault(String variableName, String projectName, String envName, Object defaultValue) {
        return withProject(projectName, Lambda.cascade(envName, getEnv().getName())).getVariableOrDefault(variableName, defaultValue);
    }

    default Variable<?> determineDeclaredVariable(String variableName, String projectName, String envName) {
        Project p = getModelTable().getProjects().get(projectName);
        return p.getDeclaredVarsForEnv(getModelTable().getEnv(envName)).get(variableName);
    }

    default Object getProjectDeclaredVariable(String variableName) {
        return getProjectDeclaredVariable(variableName, getProject().getProjectName());
    }

    default Object getProjectDeclaredVariable(String variableName, String projectName) {
        return getProjectDeclaredVariable(variableName, projectName, getEnv().getName());
    }

    default Object getProjectDeclaredVariable(String variableName, String projectName, String envName) {
        Variable<?> v = determineDeclaredVariable(variableName, projectName, envName);
        if (v == null) {
            throw new MissingVariableException(variableName);
        }
        return withProject(projectName, envName).getVariable(v);
    }

    default Object getProjectDeclaredVariableOrDefault(String variableName) {
        return getProjectDeclaredVariableOrDefault(variableName, getProject().getProjectName());
    }

    default Object getProjectDeclaredVariableOrDefault(String variableName, String projectName) {
        return getProjectDeclaredVariableOrDefault(variableName, projectName, getEnv().getName());
    }

    default Object getProjectDeclaredVariableOrDefault(String variableName, String projectName, String envName) {
        Variable<?> v = determineDeclaredVariable(variableName, projectName, envName);
        if (v == null) {
            return null;
        }
        return withProject(projectName, envName).getVariable(v);
    }

    default List<Project> filterProjects(Predicate<Job> predicate) {
        List<Project> list = new ArrayList<>();
        for (Job job : getModelTable().getJobs().getJobsInEnv(getEnv().getName())) {
            if (predicate.test(job)) {
                list.add(job.getProject());
            }
        }
        return list;
    }

    default List<Job> filterJobs(Predicate<Job> predicate) {
        List<Job> list = new ArrayList<>();
        for (Job job : getModelTable().getJobs().getJobsInEnv(getEnv().getName())) {
            if (predicate.test(job)) {
                list.add(job);
            }
        }
        return list;
    }

    default Collection<Project> getChildProjects(String... parentProjectNames) {
        List<Project> list = new ArrayList<>();
        for (Project p : getModelTable().getProjects()) {
            if (Lambda.all(parentProjectNames, parent -> !p.getProjectName().equals(parent) && p.belongsTo(parent))) {
                list.add(p);
            }
        }
        return list;
    }

    default VariableResolving<?> determineVariableInProjectVars(VariableName name) {
        Variable<?> var = getProjectVars().get(name);
        return var != null ? new VariableResolving<>(var, getResolveContext()) : null;
    }

    /**
     * determine which variable used to resolve, in groups
     *
     * @param name variable name
     * @return variable instance
     */
    @Nullable
    default VariableResolving<?> determineVariableInGroups(VariableName name) {
        String leftmost = name.first();

        List<Project> foundInProjects = new ArrayList<>(1);
        VariableResolving<?> resolving = null;

        for (int i = leftmost.indexOf(Project.VARIABLE_GROUP_SEPARATOR);
             i > 0;
             i = leftmost.indexOf(Project.VARIABLE_GROUP_SEPARATOR, i + 1)) {

            String group = leftmost.substring(0, i);
            List<Project> ps = getModelTable().getProjects().findByVariableGroup(group);
            if (ps == null) continue;

            String nextName = name.first().substring(group.length() + 1);

            VariableName newName;
            if (name.path.length > 1) {
                String[] path = name.path.clone();
                path[0] = nextName;
                newName = new VariableName(path);
            } else {
                newName = new VariableName(new String[]{nextName});
            }

            for (Project p : ps) {
                VariableResolving<?> now = withProject(p, getEnv()).determineVariableInProjectVars(newName);

                if (now != null) {
                    resolving = now;
                    foundInProjects.add(p);
                }
            }
        }

        if (foundInProjects.isEmpty()) {
            return null;
        }

        if (foundInProjects.size() > 1) {
            List<String> projectNames =  Lambda.map(
                foundInProjects,
                p -> String.format("%s[%s]", p.getProjectName(), p.getVariableGroup())
            );

            throw new InvalidVariableDefinitionException(name.toString(),
                "variable is defined in more than one projects: " + String.join(",", projectNames));
        }

        return resolving;
    }
}
