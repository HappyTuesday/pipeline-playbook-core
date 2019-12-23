package com.yit.deploy.core.storage.persistent;

import com.yit.deploy.core.records.BuildRecord;

import java.util.List;

public interface BuildMapper {
    void addBuild(BuildRecord record);

    void updateProjectCommitToBuild(BuildRecord record);

    void finishBuild(BuildRecord record);

    BuildRecord getBuild(long buildId);

    BuildRecord getJobLastBuild(String jobName);

    List<BuildRecord> queryBuilds(long from,
                                  long to,
                                  String jobName,
                                  String envName,
                                  String projectName,
                                  Boolean finished,
                                  Boolean failed,
                                  String failedType);

    long queryBuildsCount(String jobName,
                                  String envName,
                                  String projectName,
                                  Boolean finished,
                                  Boolean failed,
                                  String failedType);
}
