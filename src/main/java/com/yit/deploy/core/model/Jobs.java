package com.yit.deploy.core.model;

import com.yit.deploy.core.exceptions.IllegalConfigException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Jobs {

    /**
     * env -> project -> job mapping
     */
    private final Map<String, Map<String, Job>> map;

    private final Map<String, Job> jobMap;

    public Jobs(DeployModelTable modelTable) {
        this.map = new HashMap<>(modelTable.getEnvs().size());
        this.jobMap = new HashMap<>(modelTable.getEnvs().size() * modelTable.getProjects().size());

        for (Environment env : modelTable.getEnvs()) {
            if (env.isAbstracted()) {
                continue;
            }

            for (Project project : modelTable.getProjects()) {
                if (project.isAbstracted()) {
                    continue;
                }

                Job job = new Job(env, modelTable.getPlaybooks(), project, modelTable);
                if (job.isDisabled()) {
                    continue;
                }

                this.map.computeIfAbsent(env.getName(), e -> new HashMap<>(modelTable.getProjects().size()))
                    .put(project.getProjectName(), job);

                if (this.jobMap.put(job.getJobName(), job) != null) {
                    throw new IllegalConfigException("duplicated job " + job.getJobName());
                }
            }
        }
    }

    public void initialize() {
        for (Map<String, Job> m : map.values()) {
            for (Job job : m.values()) {
                if (job.isDisabled()) continue;

                job.initialize();
            }
        }
    }

    public Job get(String env, String project) {
        if (env == null || project == null) {
            return null;
        }

        Map<String, Job> pm = map.get(env);
        if (pm == null) {
            throw new IllegalConfigException("environment " + env + " does not contain any job");
        }

        Job job = pm.get(project);
        if (job == null) {
            throw new IllegalConfigException("project " + project + " does not exist in environment " + env);
        }
        return job;
    }

    public Job find(String env, String project) {
        if (env == null || project == null) {
            return null;
        }

        Map<String, Job> pm = map.get(env);
        if (pm == null) {
            return null;
        }

        Job job = pm.get(project);
        if (job == null) {
            return null;
        }

        return job;
    }

    public Job findByName(String jobName) {
        return jobMap.get(jobName);
    }

    public Job getByName(String jobName) {
        Job job = this.findByName(jobName);
        if (job == null) {
            throw new IllegalConfigException("could not find job " + jobName);
        }
        return job;
    }

    public Collection<Job> getJobsInEnv(String envName) {
        return map.get(envName).values();
    }
}
