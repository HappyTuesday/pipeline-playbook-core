package com.yit.deploy.core.parameters.inventory;

import com.google.common.base.Strings;
import com.yit.deploy.core.diff.DetailDiff;
import com.yit.deploy.core.diff.Datum;
import com.yit.deploy.core.diff.ExcludeDiff;
import com.yit.deploy.core.exceptions.DeployException;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.JsonSupport;
import com.yit.deploy.core.model.StatusCode;
import com.yit.deploy.core.utils.GitUtils;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@DetailDiff
public class DeployItem implements JsonSupport<DeployItem>, Datum {

    private static final Logger LOGGER = Logger.getLogger(DeployItem.class.getName());

    private String id;
    private String projectName;
    private String searchScript;
    private String searchScriptDescription;
    @ExcludeDiff
    private String searchScriptExecutionError;
    @NeedVerify
    private List<String> tags;
    @NeedVerify
    private List<String> skipTags;
    @NeedVerify
    private List<String> servers;
    @NeedVerify
    private Map<String, Object> parameters;
    @ExcludeDiff
    private Project project;
    @NeedVerify
    private List<String> dependencies = new ArrayList<>();
    @ExcludeDiff
    private int projectCount;
    private String syncMessage;
    private String verifyMessage;
    private String confirmedBy;
    private String owner;

    public void initialize(Environment environment, DeployPlan plan) {
        if (id == null) {
            id = projectName;
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
        }
        Project oldProject = project;
        if (!Strings.isNullOrEmpty(projectName)) {
            project = environment.findProject(projectName);
            projectCount = 1;
        } else if (!Strings.isNullOrEmpty(searchScript)) {
            Environment.SearchScriptExecutionResult er = environment.executeSearchScript(plan, searchScript);
            if (er.exception == null) {
                projectCount = er.projects.size();
                if (er.projects.size() == 1) {
                    project = er.projects.get(0);
                } else {
                    project = Project.zip(er.projects);
                }
                searchScriptExecutionError = null;
            } else {
                searchScriptExecutionError = er.exception.getMessage().replace("startup failed:\nScript1.groovy: ", "script compile error:\n");
            }
        }

        if (project == null) {
            project = new Project();
        }

        Map<String, Object> newParameters = new HashMap<>();
        for (ProjectParameter pp : project.getParameters()) {
            if (pp.getId() == null) {
                pp.setId(pp.getParameterName());
            }
            Object value;
            if (parameters != null && parameters.containsKey(pp.getParameterName())) {
                value = parameters.get(pp.getParameterName());
            } else {
                value = pp.getDefaultValue();
            }
            newParameters.put(pp.getParameterName(), value);
        }
        parameters = newParameters;

        // Reset tags, skip tags and servers
        // The thought of the following code is that:
        // 1. We will call call this code before saving it to disk, along with its project
        // 2. After loading it from disk, the corresponding project may have been changed.
        // 3. But the project along saved is the changed and we use oldProject to represent it
        // 4. If the tags is equal to the tags defined in oldProject, so it should be set to the new tags in new project
        // 5. Others the same.
        if (oldProject != null) {
            tags = intersectList(tags, oldProject.getTags());
            skipTags = intersectList(skipTags, oldProject.getSkipTags());
            servers = intersectList(servers, oldProject.getServers());
        }
        if (tags == null || oldProject != null && tags.size() == oldProject.getTags().size()) {
            tags = project.getTags();
        }
        if (skipTags == null || oldProject != null && Lambda.equalsIgnoreOrder(skipTags, oldProject.getSelectedSkipTags())) {
            skipTags = project.getSelectedSkipTags();
        }
        if (servers == null || oldProject != null && servers.size() == oldProject.getServers().size()) {
            servers = project.getServers();
        }

        tags = intersectList(project.getTags(), tags);
        skipTags = intersectList(project.getSkipTags(), skipTags);
        servers = intersectList(project.getServers(), servers);
    }

    List<DeployItem> expand(Environment environment, DeployPlan plan) {
        List<DeployItem> result = new ArrayList<>();
        if (!Strings.isNullOrEmpty(projectName)) {
            if (!Strings.isNullOrEmpty(project.getProjectName())) {
                result.add(this);
            }
        } else if (!Strings.isNullOrEmpty(searchScript)) {
            Environment.SearchScriptExecutionResult er = environment.executeSearchScript(plan, searchScript);
            if (er.exception == null) {
                for (Project project : er.projects) {
                    DeployItem item = new DeployItem();
                    item.projectName = project.getProjectName();
                    item.tags = tags;
                    item.skipTags = skipTags;
                    item.servers = servers;
                    item.parameters = this.parameters;
                    item.dependencies = dependencies;
                    item.verifyMessage = verifyMessage;
                    item.confirmedBy = confirmedBy;
                    item.initialize(environment, plan);
                    result.add(item);
                }
            } else {
                this.searchScriptExecutionError = er.exception.getMessage();
                result.add(this);
            }
        }
        return result;
    }

    void prepareForDeploy(DeployPlan plan) {
        if (tags == null || tags.size() == 0) {
            tags = new ArrayList<>(project.getTags());
        }

        if (skipTags == null) {
            skipTags = new ArrayList<>(project.getSelectedSkipTags());
        }

        if (plan.getExtraTasksToSkip() != null) {
            Lambda.uniqueAdd(skipTags, plan.getExtraTasksToSkip());
        }

        if (servers == null || servers.size() == 0) {
            servers = new ArrayList<>(project.getServers());
        }
    }

    private static <T> List<T> intersectList(List<T> list1, List<T> list2) {
        if (list1 == null && list2 == null) return null;
        List<T> result = new ArrayList<>();
        if (list1 == null || list2 == null) return result;
        for (T t : list1) {
            if (list2.contains(t) && ! result.contains(t)) {
                result.add(t);
            }
        }
        return result;
    }

    public void verifyForDeploy(int planId) {
        if (!Lambda.isNullOrEmpty(verifyMessage) && Lambda.isNullOrEmpty(confirmedBy)) {
            throw new DeployException(new StatusCode(400, "Project " + projectName + " is not confirmed in plan #" + planId));
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getSearchScript() {
        return searchScript;
    }

    public void setSearchScript(String searchScript) {
        this.searchScript = searchScript;
    }

    public String getSearchScriptDescription() {
        return searchScriptDescription;
    }

    public void setSearchScriptDescription(String searchScriptDescription) {
        this.searchScriptDescription = searchScriptDescription;
    }

    public String getSearchScriptExecutionError() {
        return searchScriptExecutionError;
    }

    public void setSearchScriptExecutionError(String searchScriptExecutionError) {
        this.searchScriptExecutionError = searchScriptExecutionError;
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

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public int getProjectCount() {
        return projectCount;
    }

    public void setProjectCount(int projectCount) {
        this.projectCount = projectCount;
    }

    public String getSyncMessage() {
        return syncMessage;
    }

    public void setSyncMessage(String syncMessage) {
        this.syncMessage = syncMessage;
    }

    public String getVerifyMessage() {
        return verifyMessage;
    }

    public void setVerifyMessage(String verifyMessage) {
        this.verifyMessage = verifyMessage;
    }

    public String getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(String confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
