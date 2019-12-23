package com.yit.deploy.core.parameters.inventory;

import com.google.common.base.Strings;
import com.yit.deploy.core.model.DeployModelTable;
import com.yit.deploy.core.model.Job;
import com.yit.deploy.core.model.JsonSupport;
import groovy.lang.GroovyShell;

import java.util.ArrayList;
import java.util.List;

public class Environment implements JsonSupport<Environment> {
    private String name;
    private List<Project> projects = new ArrayList<>();

    public Environment() {
    }

    public Environment(String name, Iterable<Job> jobs) {
        this.name = name;
        for (Job job : jobs) {
            this.projects.add(new Project(job));
        }
    }

    public Project findProject(String projectName) {
        for (Project p : projects) {
            if (p.getProjectName().equals(projectName)) {
                return p;
            }
        }
        return null;
    }

    public SearchScriptExecutionResult executeSearchScript(DeployPlan plan, String searchScript) {
        List<Project> searchedProjects = new ArrayList<>();
        SearchScriptExecutionResult executionResult = new SearchScriptExecutionResult();
        executionResult.projects = searchedProjects;

        if (Strings.isNullOrEmpty(searchScript)) return executionResult;

        GroovyShell shell = new GroovyShell();
        shell.setVariable("plan", this);
        shell.setVariable("env", this);
        shell.setVariable("projects", projects);

        Object result;
        try {
            result = shell.evaluate(searchScript);
        } catch (Exception e) {
            executionResult.exception = e;
            return executionResult;
        }

        if (result instanceof Project) {
            searchedProjects.add((Project) result);
            return executionResult;
        }

        if (!(result instanceof List<?>)) return executionResult;
        List list = (List) result;
        for (Object projectObject : list) {
            if (! (projectObject instanceof Project)) continue;
            Project project = (Project) projectObject;

            boolean exist = false;
            for (DeployItem item : plan.getItems()) {
                if (project.getProjectName().equals(item.getProjectName())) {
                    exist = true;
                    break;
                }
            }
            if (exist) continue;
            for (Project p : searchedProjects) {
                if (project.getProjectName().equals(p.getProjectName())) {
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                searchedProjects.add(project);
            }
        }

        return executionResult;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }

    static class SearchScriptExecutionResult {
        List<Project> projects;
        Exception exception;
    }
}
