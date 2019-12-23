package com.yit.deploy.core.parameters.inventory;

import com.yit.deploy.core.model.Job;
import com.yit.deploy.core.model.JsonSupport;

import java.util.*;

public class Project implements JsonSupport<Project> {
    private String projectName = "";
    private String jobName = "";
    private String sectionName = "";
    private List<String> tags = new ArrayList<>();
    private List<String> skipTags = new ArrayList<>();
    private List<String> selectedSkipTags = new ArrayList<>();
    private List<String> servers = new ArrayList<>();
    private int jobOrder;
    private List<String> containerLabels = new ArrayList<>();
    private List<ProjectParameter> parameters = new ArrayList<>();
    private List<String> dependencies = new ArrayList<>();
    private String gitRepositoryUrl;

    public Project() {
    }

    public Project(Job job) {
        this.projectName = job.getProjectName();
        this.jobName = job.getJobName();
        this.sectionName = job.getSectionName();
        this.tags = job.getPlays();
        this.skipTags = job.getTasks();
        this.selectedSkipTags = job.getTasksToSkip();
        this.servers = job.getServers();
        this.jobOrder = job.getJobOrder();
        this.containerLabels = job.getContainerLabels();
        for (com.yit.deploy.core.model.ProjectParameter p : job.getUserParameters()) {
            ProjectParameter q = new ProjectParameter();
            q.setParameterName(p.getParameterName());
            q.setDefaultValue(p.getDefaultValue());
            q.setDescription(p.getDescription());
            q.setHidden(p.isHidden());
            q.setRequired(p.isRequired());
            q.setType(p.getType());
            this.parameters.add(q);
        }
        this.dependencies = job.getDependencies();
        this.gitRepositoryUrl = job.getGitRepositoryUrl();
    }

    /**
     * collect properties from projects to this
     * @param projects
     */
    public static Project zip(List<Project> projects) {
        Project p = new Project();
        boolean first = true;
        Set<String> sections = new HashSet<>();
        for (Project q : projects) {
            String prefix = first ? "" : ",";

            p.setProjectName(concatString(p.getProjectName(), prefix, q.getProjectName()));
            p.setJobName(concatString(p.getJobName(), prefix, q.getJobName()));
            if (sections.add(q.getSectionName())) p.setSectionName(concatString(p.getSectionName(), prefix, q.getSectionName()));
            p.setTags(mergeList(p.getTags(), q.getTags()));
            p.setSkipTags(mergeList(p.getSkipTags(), q.getSkipTags()));
            p.setSelectedSkipTags(mergeList(p.getSelectedSkipTags(), q.getSelectedSkipTags()));
            p.setServers(mergeList(p.getServers(), q.getServers()));
            p.setJobOrder(Math.max(p.getJobOrder(), q.getJobOrder()));
            p.setContainerLabels(mergeList(p.getContainerLabels(), q.getContainerLabels()));

            for (ProjectParameter qp : q.getParameters()) {
                boolean foundParameter = false;
                for (ProjectParameter pp : p.getParameters()) {
                    if (pp.getParameterName().equals(qp.getParameterName())) {
                        foundParameter = true;
                        break;
                    }
                }
                if (!foundParameter) {
                    p.getParameters().add(qp);
                }
            }

            first = false;
        }
        return p;
    }

    private static String concatString(String ... args) {
        StringBuilder a = new StringBuilder();
        for (String s : args) {
            if (s != null) a.append(s);
        }
        return a.toString();
    }

    private static <T> List<T> mergeList(List<T> list1, List<T> list2) {
        if (list1 == null && list2 == null) return null;
        List<T> result = new ArrayList<>();
        if (list1 != null) {
            result.addAll(list1);
        }
        if (list2 != null) {
            for (T t : list2) {
                if (!result.contains(t)) result.add(t);
            }
        }
        return result;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getSkipTags() {
        return skipTags;
    }

    public void setSkipTags(List<String> skipTags) {
        this.skipTags = skipTags;
    }

    public List<String> getSelectedSkipTags() {
        return selectedSkipTags;
    }

    public void setSelectedSkipTags(List<String> selectedSkipTags) {
        this.selectedSkipTags = selectedSkipTags;
    }

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }

    public int getJobOrder() {
        return jobOrder;
    }

    public void setJobOrder(int jobOrder) {
        this.jobOrder = jobOrder;
    }

    public List<String> getContainerLabels() {
        return containerLabels;
    }

    public void setContainerLabels(List<String> containerLabels) {
        this.containerLabels = containerLabels;
    }

    public List<ProjectParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<ProjectParameter> parameters) {
        this.parameters = parameters;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public String getGitRepositoryUrl() {
        return gitRepositoryUrl;
    }

    public void setGitRepositoryUrl(String gitRepositoryUrl) {
        this.gitRepositoryUrl = gitRepositoryUrl;
    }
}
