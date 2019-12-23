package com.yit.deploy.core.records;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BuildRecord {
    private long id;
    private Long parentId;
    private String deployUserName;
    private String jobName;
    private String envName;
    private String projectName;
    private boolean jenkinsBuild;

    private Long recordCommitId;
    private String configCommitHash;

    private List<String> plays;
    private List<String> tasksToSkip;
    private List<String> servers;
    private LinkedHashMap<String, Object> userParameters;

    private Date startedTime;
    private Date finishedTime;
    private Boolean failed;
    private String failedType;
    private String failedMessage;

    private String projectCommitBranch;
    private String projectCommitHash;
    private String projectCommitEmail;
    private String projectCommitDetail;
    private Date projectCommitDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getDeployUserName() {
        return deployUserName;
    }

    public void setDeployUserName(String deployUserName) {
        this.deployUserName = deployUserName;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public boolean isJenkinsBuild() {
        return jenkinsBuild;
    }

    public void setJenkinsBuild(boolean jenkinsBuild) {
        this.jenkinsBuild = jenkinsBuild;
    }

    public Long getRecordCommitId() {
        return recordCommitId;
    }

    public void setRecordCommitId(Long recordCommitId) {
        this.recordCommitId = recordCommitId;
    }

    public String getConfigCommitHash() {
        return configCommitHash;
    }

    public void setConfigCommitHash(String configCommitHash) {
        this.configCommitHash = configCommitHash;
    }

    public List<String> getPlays() {
        return plays;
    }

    public void setPlays(List<String> plays) {
        this.plays = plays;
    }

    public List<String> getTasksToSkip() {
        return tasksToSkip;
    }

    public void setTasksToSkip(List<String> skippedTasks) {
        this.tasksToSkip = skippedTasks;
    }

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }

    public LinkedHashMap<String, Object> getUserParameters() {
        return userParameters;
    }

    public void setUserParameters(LinkedHashMap<String, Object> userParameters) {
        this.userParameters = userParameters;
    }

    public Date getStartedTime() {
        return startedTime;
    }

    public void setStartedTime(Date startedTime) {
        this.startedTime = startedTime;
    }

    public Date getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(Date finishedTime) {
        this.finishedTime = finishedTime;
    }

    public Boolean getFailed() {
        return failed;
    }

    public void setFailed(Boolean failed) {
        this.failed = failed;
    }

    public String getFailedType() {
        return failedType;
    }

    public void setFailedType(String failedType) {
        this.failedType = failedType;
    }

    public String getFailedMessage() {
        return failedMessage;
    }

    public void setFailedMessage(String failedMessage) {
        this.failedMessage = failedMessage;
    }

    public String getProjectCommitBranch() {
        return projectCommitBranch;
    }

    public void setProjectCommitBranch(String projectCommitBranch) {
        this.projectCommitBranch = projectCommitBranch;
    }

    public String getProjectCommitHash() {
        return projectCommitHash;
    }

    public void setProjectCommitHash(String projectCommitHash) {
        this.projectCommitHash = projectCommitHash;
    }

    public String getProjectCommitEmail() {
        return projectCommitEmail;
    }

    public void setProjectCommitEmail(String projectCommitEmail) {
        this.projectCommitEmail = projectCommitEmail;
    }

    public String getProjectCommitDetail() {
        return projectCommitDetail;
    }

    public void setProjectCommitDetail(String projectCommitDetail) {
        this.projectCommitDetail = projectCommitDetail;
    }

    public Date getProjectCommitDate() {
        return projectCommitDate;
    }

    public void setProjectCommitDate(Date projectCommitDate) {
        this.projectCommitDate = projectCommitDate;
    }
}
