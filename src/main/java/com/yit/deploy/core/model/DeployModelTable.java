package com.yit.deploy.core.model;

import com.yit.deploy.core.config.DeployConfig;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.info.*;
import com.yit.deploy.core.records.Commit;
import com.yit.deploy.core.records.DeployRecordTable;

public class DeployModelTable {

    private final DeployInfoTable infoTable;
    private final Commit commit;
    private final DeployConfig deployConfig;
    private final Environments envs;
    private final Playbooks playbooks;
    private final Projects projects;
    private final Jobs jobs;

    public DeployModelTable(DeployConfig deployConfig, DeployInfoTable infoTable) {
        this.commit = infoTable.getCommit();
        this.deployConfig = deployConfig;
        this.infoTable = infoTable;

        this.envs = new Environments(infoTable.getEnvs());
        if (!this.envs.hasRootEnv()) {
            throw new IllegalConfigException("root environment is not defined");
        }

        this.playbooks = new Playbooks(infoTable.getPlaybooks());

        this.projects = new Projects(infoTable.getProjects(), this.envs);

        this.jobs = new Jobs(this);
        this.jobs.initialize();
    }

    public DeployInfoTable getInfoTable() {
        return infoTable;
    }

    public Commit getCommit() {
        return commit;
    }

    public DeployConfig getDeployConfig() {
        return deployConfig;
    }

    public Job getJob(String projectName, String envName) {
        return jobs.get(envName, projectName);
    }

    public Job getJob(Project project, Environment env) {
        return getJob(project.getProjectName(), env.getName());
    }

    public Job findJob(String projectName, String envName) {
        return jobs.find(envName, projectName);
    }

    public Job findJobByName(String jobName) {
        return jobs.findByName(jobName);
    }

    public Job getJobByName(String jobName) {
        return jobs.getByName(jobName);
    }

    public Job findJobWithSharing(String projectName, String envName) {
        Job job = jobs.find(envName, projectName);
        if (job != null) {
            return job;
        }
        Project p = projects.get(projectName);
        String shared = p.getSharing().get(envName);
        if (shared != null && !shared.equals(envName)) {
            return findJobWithSharing(projectName, shared);
        }
        return null;
    }

    public Environments getEnvs() {
        return envs;
    }

    public Environment getEnv(String envName) {
        return envs.get(envName);
    }

    public Playbooks getPlaybooks() {
        return playbooks;
    }

    public Projects getProjects() {
        return projects;
    }

    public Jobs getJobs() {
        return jobs;
    }
}
