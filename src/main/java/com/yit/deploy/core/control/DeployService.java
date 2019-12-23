package com.yit.deploy.core.control;

import com.yit.deploy.core.config.ConfigProject;
import com.yit.deploy.core.config.DeployConfig;
import com.yit.deploy.core.exceptions.DeployException;
import com.yit.deploy.core.info.DeployTableResponse;
import com.yit.deploy.core.info.DeployInfoTable;
import com.yit.deploy.core.model.DeployModelTable;
import com.yit.deploy.core.records.*;
import com.yit.deploy.core.storage.DeployStorage;
import com.yit.deploy.core.storage.StorageConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

public class DeployService {

    private static Logger logger = Logger.getLogger(DeployService.class.getName());

    private static DeployService instance;

    public static synchronized DeployService getInstance(ConfigProject configProject, StorageConfig storageConfig) {
        if (instance == null ||
            !instance.configProject.equals(configProject) ||
            !instance.storageConfig.equals(storageConfig)) {

            instance = new DeployService(configProject, storageConfig);
        }

        return instance;
    }

    private final ConfigProject configProject;
    private final StorageConfig storageConfig;
    private final DeployStorage deployStorage;

    private DeployConfig deployConfig;
    private List<DeployRecordTable> appliedCommits = new ArrayList<>();
    private DeployInfoTable infoTable;
    private DeployModelTable modelTable;
    private DeployTableResponse tableResponse;

    public DeployService(ConfigProject configProject, StorageConfig storageConfig) {
        this.configProject = configProject;
        this.storageConfig = storageConfig;
        this.deployStorage = DeployStorage.getInstance(storageConfig);
    }

    public DeployConfig getDeployConfig() {
        return deployConfig;
    }

    public StorageConfig getStorageConfig() {
        return storageConfig;
    }

    public synchronized DeployInfoTable getInfoTable() {
        String branchName = storageConfig.defaultBranch;
        if (branchName == null || branchName.isEmpty()) {
            branchName = "master";
        }

        DeployConfig deployConfig =  DeployConfig.getInstance(configProject);
        long lastCommitId;
        if (this.appliedCommits.isEmpty()) {
            lastCommitId = 0;
        } else {
            lastCommitId = this.appliedCommits.get(this.appliedCommits.size() - 1).getCommit().getId();
        }
        List<DeployRecordTable> newCommits = this.deployStorage.getLatestCommits(lastCommitId, branchName);

        if (!newCommits.isEmpty() || this.deployConfig != deployConfig) {
            DeployInfoTable infoTable = this.infoTable;
            if (infoTable == null || this.deployConfig != deployConfig) {
                // first time, or new commits of deploy config project were found
                infoTable = deployConfig.getInfoTable();
                // since our base has changed, we must rebuild all
                infoTable = infoTable.withRecordTables(this.appliedCommits);
            }

            infoTable = infoTable.withRecordTables(newCommits);

            // until now, we have not changed any of these fields, be sure we have not.
            // since the previous code may fail, we can retry next time if nothing changed by us.

            this.deployConfig = deployConfig;
            this.appliedCommits.addAll(newCommits);
            this.infoTable = infoTable;
        }

        return this.infoTable;
    }

    public synchronized DeployTableResponse getDeployTableResponse() {
        DeployModelTable now = this.getModelTable();
        if (this.tableResponse == null || this.tableResponse.getInfoTable() != now.getInfoTable()) {
            // the this.deployConfig and this.infoTable have been changed by this.getInfoTable()
            this.tableResponse = new DeployTableResponse(this.storageConfig.defaultBranch, now);
        }

        return this.tableResponse;
    }

    public synchronized DeployTableResponse getDeployTableResponse(String branchName) {
        if (branchName == null || branchName.isEmpty() || branchName.equals(this.storageConfig.defaultBranch)) {
            return this.getDeployTableResponse();
        }

        DeployConfig deployConfig =  DeployConfig.getInstance(configProject);
        List<DeployRecordTable> commits = this.deployStorage.getLatestCommits(0, branchName);

        DeployInfoTable infoTable = deployConfig.getInfoTable().withRecordTables(commits);
        return new DeployTableResponse(branchName, new DeployModelTable(deployConfig, infoTable));
    }

    public synchronized DeployModelTable getModelTable() {
        DeployInfoTable now = this.getInfoTable();
        if (this.modelTable == null || this.modelTable.getInfoTable() != now) {
            // the this.deployConfig and this.infoTable have been changed by this.getInfoTable()
            this.modelTable = new DeployModelTable(this.deployConfig, now);
        }

        return this.modelTable;
    }

    public void saveDraft(String targetBranch, DeployRecordTable delta) {
        Commit commit = delta.getCommit();
        long baseCommitId = commit.getParentId() == null ? 0: commit.getParentId();
        List<DeployRecordTable> previous = deployStorage.getCommits(baseCommitId, 0);
        List<DeployRecordTable> latest = deployStorage.getLatestCommits(baseCommitId, targetBranch);
        if (!latest.isEmpty()) {
            throw new DeployException(400, "new commits found from " + latest.get(latest.size() -1).getCommit().getAuthor());
        }

        DeployConfig deployConfig =  DeployConfig.getInstance(configProject);
        DeployInfoTable infoTable = deployConfig.getInfoTable().withRecordTables(previous).withRecordTable(delta);
        // validate the final info table
        new DeployModelTable(deployConfig, infoTable);

        deployStorage.appendCommitDetail(targetBranch, delta);
    }

    public List<Branch> getBranches() {
        return deployStorage.loadBranches();
    }

    public List<DeployRecordTable> loadCommits(long from, int count) {
        return deployStorage.loadCommits(from, count);
    }

    public void addBuild(BuildRecord record) {
        deployStorage.addBuild(record);
    }

    public void updateProjectCommitToBuild(BuildRecord record) {
        deployStorage.updateProjectCommitToBuild(record);
    }

    public void finishBuild(BuildRecord record) {
        deployStorage.finishBuild(record);
    }

    public BuildRecord getBuild(long buildId) {
        return deployStorage.getBuild(buildId);
    }

    public BuildRecord getJobLastBuild(String jobName) {
        return deployStorage.getJobLastBuild(jobName);
    }

    public Page<BuildRecord> queryBuilds(int pageIndex,
                                         int pageSize,
                                         String jobName,
                                         String envName,
                                         String projectName,
                                         Boolean finished,
                                         Boolean failed,
                                         String failedType) {
        return deployStorage.queryBuilds(pageIndex, pageSize, jobName, envName, projectName,
            finished, failed, failedType);
    }

    public String getConfigValue(String envName, String projectName, String namespace, String key, String locker) {
        return deployStorage.getConfigValue(envName, projectName, namespace, key, locker);
    }

    public void updateConfigValue(String envName, String projectName, String namespace, String key, String locker, Function<String, String> f) {
        deployStorage.updateConfigValue(envName, projectName, namespace, key, locker, f);
    }
}
