package com.yit.deploy.core.info;

import com.yit.deploy.core.config.DeployConfig;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.model.*;
import com.yit.deploy.core.records.*;

import java.util.*;

/**
 * a collection of the deploy info models,
 * used both for collecting info from executing config project scripts and for serializing to client
 */
public class DeployInfoTable implements JsonSupport<DeployInfoTable> {

    private final Commit commit;
    private final Map<String, EnvironmentInfo> envs;
    private final Map<String, PlaybookInfo> playbooks;
    private final Map<String, ProjectInfo> projects;

    public DeployInfoTable(Commit commit,
                           Map<String, EnvironmentInfo> envs,
                           Map<String, PlaybookInfo> playbooks,
                           Map<String, ProjectInfo> projects) {
        this.commit = commit;
        this.envs = envs;
        this.playbooks = playbooks;
        this.projects = projects;
    }

    public DeployInfoTable(DeployInfoTable that) {
        this.commit = that.commit;
        this.envs = that.envs;
        this.playbooks = that.playbooks;
        this.projects = that.projects;
    }

    public DeployInfoTable(DeployConfig deployConfig) {
        this.commit = new Commit();
        this.envs = Environments.load(deployConfig);
        Environments envs = new Environments(this.envs);
        if (!envs.hasRootEnv()) {
            throw new IllegalConfigException("root environment is not defined");
        }
        this.playbooks = Playbooks.load(deployConfig, envs);
        Playbooks playbooks = new Playbooks(this.playbooks);
        this.projects = Projects.load(deployConfig, envs, playbooks);
    }

    public DeployInfoTable withRecordTables(List<DeployRecordTable> recordTables) {
        DeployInfoTable target = this;
        for (DeployRecordTable table : recordTables) {
            target = target.withRecordTable(table);
        }
        return target;
    }

    public DeployInfoTable withRecordTable(DeployRecordTable recordTable) {
        Commit targetCommit = this.commit;

        if (recordTable.getCommit() != null) {
            targetCommit = recordTable.getCommit();
        }

        Map<String, EnvironmentInfo> targetEnvs = this.envs;
        if (recordTable.getEnvs() != null) {
            targetEnvs = new HashMap<>(this.envs);

            RecordTarget.applyRecordsToMap(
                recordTable.getEnvs(),
                EnvironmentRecord::getName,
                targetEnvs,
                r -> new EnvironmentInfo(r.getName())
            );
        }

        if (recordTable.getHosts() != null) {
            if (targetEnvs == this.envs) {
                targetEnvs = new HashMap<>(this.envs);
            }

            Map<String, List<HostRecord>> map = new HashMap<>();
            for (HostRecord record : recordTable.getHosts()) {
                map.computeIfAbsent(record.getEnv(), k -> new LinkedList<>()).add(record);
            }

            for (Map.Entry<String, List<HostRecord>> entry : map.entrySet()) {
                EnvironmentInfo env = targetEnvs.get(entry.getKey());
                if (env == null) {
                    throw new IllegalConfigException("could not find env " + entry.getKey());
                }
                targetEnvs.put(entry.getKey(), env.withHostRecords(entry.getValue()));
            }
        }

        if (recordTable.getHostGroups() != null) {
            if (targetEnvs == this.envs) {
                targetEnvs = new HashMap<>(this.envs);
            }

            Map<String, List<HostGroupRecord>> map = new HashMap<>();
            for (HostGroupRecord record : recordTable.getHostGroups()) {
                map.computeIfAbsent(record.getEnv(), k -> new LinkedList<>()).add(record);
            }

            for (Map.Entry<String, List<HostGroupRecord>> entry : map.entrySet()) {
                EnvironmentInfo env = targetEnvs.get(entry.getKey());
                if (env == null) {
                    throw new IllegalConfigException("could not find env " + entry.getKey());
                }
                targetEnvs.put(entry.getKey(), env.withHostGroupRecords(entry.getValue()));
            }
        }

        if (recordTable.getAssigns() != null) {
            Map<String, List<Assignment>> map = new HashMap<>();
            for (Assignment assign : recordTable.getAssigns()) {
                if (assign.getScope() == null || assign.getScope() == AssignmentScope.environment) {
                    String envName = assign.getEnvName() == null ? Environments.ROOT_ENVIRONMENT_NAME : assign.getEnvName();
                    map.computeIfAbsent(envName, k -> new LinkedList<>()).add(assign);
                }
            }

            if (!map.isEmpty()) {
                if (targetEnvs == this.envs) {
                    targetEnvs = new HashMap<>(this.envs);
                }

                for (Map.Entry<String, List<Assignment>> entry : map.entrySet()) {
                    EnvironmentInfo env = targetEnvs.get(entry.getKey());
                    if (env == null) {
                        throw new IllegalConfigException("could not find env " + entry.getKey());
                    }
                    targetEnvs.put(entry.getKey(), env.withAssignRecords(entry.getValue()));
                }
            }
        }

        Map<String, ProjectInfo> targetProjects = this.projects;

        if (recordTable.getProjects() != null) {
            targetProjects = new HashMap<>(this.projects);

            RecordTarget.applyRecordsToMap(
                recordTable.getProjects(),
                ProjectRecord::getProjectName,
                targetProjects,
                r -> new ProjectInfo(r.getProjectName())
            );
        }

        if (recordTable.getAssigns() != null) {
            Map<String, List<Assignment>> map = new HashMap<>();
            for (Assignment assign : recordTable.getAssigns()) {
                if (assign.getScope() == AssignmentScope.project) {
                    String projectName = assign.getProjectName() == null ? Projects.ROOT_PROJECT_NAME : assign.getProjectName();
                    map.computeIfAbsent(projectName, k -> new LinkedList<>()).add(assign);
                }
            }

            if (!map.isEmpty()) {
                if (targetProjects == this.projects) {
                    targetProjects = new HashMap<>(this.projects);
                }

                for (Map.Entry<String, List<Assignment>> entry : map.entrySet()) {
                    ProjectInfo project = targetProjects.get(entry.getKey());
                    if (project == null) {
                        throw new IllegalConfigException("could not find project " + entry.getKey());
                    }

                    targetProjects.put(entry.getKey(), project.withAssignRecords(entry.getValue()));
                }
            }
        }

        return new DeployInfoTable(targetCommit, targetEnvs, this.playbooks, targetProjects);
    }

    public Commit getCommit() {
        return commit;
    }

    public Map<String, EnvironmentInfo> getEnvs() {
        return envs;
    }

    public Map<String, PlaybookInfo> getPlaybooks() {
        return playbooks;
    }

    public Map<String, ProjectInfo> getProjects() {
        return projects;
    }
}
