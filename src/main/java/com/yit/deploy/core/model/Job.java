package com.yit.deploy.core.model;

import com.yit.deploy.core.dsl.evaluate.JobEvaluationContext;
import com.yit.deploy.core.dsl.evaluate.PlaybookParameterEvaluationContext;
import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.exceptions.ExitException;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.variables.LayeredVariables;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;

public class Job {
    private final String jobName;
    private final transient LayeredVariables vars;
    private final transient DeployModelTable modelTable;
    private final Environment env;
    private final Playbook playbook;
    private final Project project;
    private final boolean disabled;

    private String groupName;
    private String sectionName;
    private int jobOrder;
    private String gitRepositoryUrl;
    private String projectGitName;
    private String defaultBranchName;
    private List<String> dependencies;
    private List<String> containerLabels;
    private String schedule;

    private List<String> allPlays;
    private List<String> plays;
    private List<String> tasks;
    private List<String> tasksToSkip;
    private List<String> servers;
    private List<ProjectParameter> userParameters;

    public Job(Environment env, Playbooks playbooks, Project project, DeployModelTable modelTable) {
        this.vars = project.getVarsForEnv(env);
        this.modelTable = modelTable;

        this.env = env;
        this.project = project;

        Map<String, Object> pps = new PlaybookParameterEvaluationContext(this.vars)
            .concreteVariableOrDefault(Project.PLAYBOOK_PARAMETERS_VARIABLE, Collections.emptyMap());
        this.playbook = playbooks.getOrCreate(project.getPlaybookName(), pps);

        JobEvaluationContext context = new JobEvaluationContext(this);
        this.jobName = (String) context.getVariable(Project.JOB_NAME_VARIABLE);

        this.disabled =
            !this.playbook.isEnabledIn(env) ||
            !project.isEnabledIn(context) ||
            project.getSharing().containsKey(env.getName());
    }

    public void initialize() {
        JobEvaluationContext context = new JobEvaluationContext(this);

        this.sectionName = context.getVariable(Project.SECTION_NAME_VARIABLE, String.class);
        if (this.sectionName == null) {
            throw new IllegalConfigException("section name is not set for job " + this.jobName);
        }

        Integer jobOrder = context.getVariable(Project.JOB_ORDER_VARIABLE, Integer.class);
        if (jobOrder == null) {
            throw new IllegalConfigException("job order is not set for job " + this.jobName);
        }
        this.jobOrder = jobOrder;

        this.groupName = (String) context.getVariableOrDefault(Project.GROUP_NAME_VARIABLE);
        this.projectGitName = (String) context.getVariableOrDefault(Project.PROJECT_GIT_NAME_VARIABLE);
        this.gitRepositoryUrl = (String) context.getVariableOrDefault(Project.GIT_REPOSITORY_URL_VARIABLE);
        this.defaultBranchName = (String) context.getVariableOrDefault(Project.DEFAULT_BRANCH_VARIABLE);
        this.dependencies = Lambda.unique(context.concreteVariableOrDefault(Project.DEPENDENCY_VARIABLE, Collections.emptyList()));
        this.containerLabels = Lambda.unique(context.concreteVariableOrDefault(Project.CONTAINER_LABELS_VARIABLE, Collections.emptyList()));
        this.schedule = (String) context.getVariableOrDefault(Project.SCHEDULE_VARIABLE);

        this.allPlays = this.playbook.getAllEnabledPlays(context);
        this.plays = this.playbook.getEnabledPlays(context, this.playbook.getDefaultScene().getPlays());
        this.tasks = this.playbook.getAllTaskTags(this, this.plays);
        List<String> requiredTasks = context.concreteVariableOrDefault(Project.REQUIRED_TASKS_VARIABLE, Collections.emptyList());
        this.tasks.removeAll(requiredTasks);
        this.tasksToSkip = context.concreteVariableOrDefault(Project.TASKS_TO_SKIP_VARIABLE, Collections.emptyList());
        this.tasksToSkip.retainAll(this.tasks);
        this.servers = this.playbook.getAllServers(context, this.plays);
        this.userParameters = this.playbook.getUserParameters(context, this.plays);
    }

    public DeployModelTable getModelTable() {
        return modelTable;
    }

    public String getJobName() {
        return jobName;
    }

    public String getProjectName() {
        return project.getProjectName();
    }

    public boolean isDisabled() {
        return disabled;
    }

    public String getEnvName() {
        return env.getName();
    }

    public Environment getEnv() {
        return env;
    }

    public Playbook getPlaybook() {
        return playbook;
    }

    public Project getProject() {
        return project;
    }

    public LayeredVariables getVars() {
        return vars;
    }

    public String getProjectGitName() {
        return projectGitName;
    }

    public String getGitRepositoryUrl() {
        return gitRepositoryUrl;
    }

    public String getDefaultBranchName() {
        return defaultBranchName;
    }

    public String getSchedule() {
        return schedule;
    }

    public List<String> getContainerLabels() {
        return containerLabels;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getSectionName() {
        return sectionName;
    }

    public int getJobOrder() {
        return jobOrder;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public List<String> getAllPlays() {
        return allPlays;
    }

    public List<String> getPlays() {
        return plays;
    }

    public List<String> getTasks() {
        return tasks;
    }

    public List<String> getTasksToSkip() {
        return tasksToSkip;
    }

    public List<String> getServers() {
        return servers;
    }

    public List<ProjectParameter> getUserParameters() {
        return userParameters;
    }

    public Object resolveVar(String variableName) {
        return new JobEvaluationContext(this).getVariable(variableName);
    }

    public <T> T resolveVarOrDefault(String variableName, T defaultValue) {
        return new JobEvaluationContext(this).getVariableOrDefault(variableName, defaultValue);
    }

    public Object concreteVar(String variableName) {
        return new JobEvaluationContext(this).concreteVariable(variableName);
    }

    public <T> T concreteVarOrDefault(String variableName, T defaultValue) {
        return new JobEvaluationContext(this).concreteVariableOrDefault(variableName, defaultValue);
    }

    public void execute(Build build) {
        List<String> invalidPlays = Lambda.except(plays, allPlays);
        if (!invalidPlays.isEmpty()) {
            build.getScript().info("plays %s are not defined in playbook", Lambda.toString(invalidPlays));
        }

        if (disabled) {
            throw new IllegalArgumentException("project " + project.getProjectName() + " is not enabled");
        }

        if (build.getJob() != this) {
            throw new IllegalArgumentException("build.job must equal to this.job");
        }

        JobExecutionContext context = new JobExecutionContext(build);

        if (build.getScript().isRoot()) {
            playbook.execute(build.getSpec(), context);
        } else {
            build.getScript().getSteps().dir(build.getScript().getWorkspace(),
                () -> playbook.execute(build.getSpec(), context));
        }
    }

    public void schedule(Consumer<ScheduleDsl> descriptor) {
        ScheduleDsl dsl = new ScheduleDsl().plays(plays).servers(servers);
        descriptor.accept(dsl);
        assert dsl._script != null && dsl._plays != null && dsl._tasksToSkip != null && dsl._servers != null && dsl._userParameterSource != null;
        if (dsl._filterPlays) {
            dsl._plays = Lambda.intersect(dsl._plays, allPlays);
        }
        Map<String, Object> parameters = new HashMap<>(dsl._userParameterSource);
        parameters.put("plays", dsl._plays);
        parameters.put("tasks_to_skip", dsl._tasksToSkip);
        parameters.put("servers", dsl._servers);

        if (disabled) {
            throw new IllegalArgumentException("job " + jobName + " is not enabled");
        }

        dsl._script.scheduleBuild(jobName, parameters, dsl._waitForCompletion, dsl._propagateErrors);
    }

    public static class ScheduleDsl implements Serializable {
        private PipelineScript _script;
        private List<String> _plays;
        private List<String> _tasksToSkip = new ArrayList<>();
        private List<String> _servers;
        private boolean _filterPlays;
        private boolean _waitForCompletion;
        private boolean _propagateErrors;
        private Map<String, Object> _userParameterSource = new HashMap<>();

        public ScheduleDsl script(PipelineScript value) {
            _script = value;
            return this;
        }

        public ScheduleDsl plays(List<String> value) {
            _plays = value;
            return this;
        }

        public ScheduleDsl plays(String ... value) {
            return plays(Arrays.asList(value));
        }

        public ScheduleDsl tasksToSkip(List<String> value) {
            _tasksToSkip = value;
            return this;
        }

        public ScheduleDsl tasksToSkip(String ... value) {
            return tasksToSkip(Arrays.asList(value));
        }

        public ScheduleDsl servers(List<String> value) {
            _servers = value;
            return this;
        }

        public ScheduleDsl servers(String ... value) {
            return servers(Arrays.asList(value));
        }

        public ScheduleDsl userParameterSource(Map<String, Object> value) {
            _userParameterSource = value;
            return this;
        }

        public ScheduleDsl filterPlays() {
            _filterPlays = true;
            return this;
        }

        public ScheduleDsl waitForCompletion() {
            _waitForCompletion = true;
            return this;
        }

        public ScheduleDsl propagateErrors() {
            _propagateErrors = true;
            return this;
        }
    }
}
