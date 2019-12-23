package com.yit.deploy.core.storage;

import com.yit.deploy.core.collections.ReverseList;
import com.yit.deploy.core.records.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class LocalDeployStorage extends DeployStorage {

    private static AtomicLong NEXT_COMMIT_ID = new AtomicLong(1);
    private static AtomicLong NEXT_BUILD_ID = new AtomicLong(1);

    /**
     * used to cache all commits of all branches
     */
    private final Map<String, Branch> branches = new ConcurrentHashMap<>();
    private final Map<Long, DeployRecordTable> recordTables = new ConcurrentHashMap<>();
    private final Map<Long, BuildRecord> builds = new ConcurrentHashMap<>();
    private final Map<Long, Config> configs = new HashMap<>();

    public LocalDeployStorage(StorageConfig config) {
        super(config);
    }

    @Override
    public List<Branch> loadBranches() {
        return new ArrayList<>(branches.values());
    }

    @Override
    public Branch loadBranch(String branchName) {
        return branches.get(branchName);
    }

    @Override
    protected DeployRecordTable loadCommitDetail(long id) {
        return recordTables.get(id);
    }

    @Override
    public void saveCommitDetail(String targetBranch, DeployRecordTable recordTable) {
        Commit commit = recordTable.getCommit();
        commit.setId(NEXT_COMMIT_ID.getAndIncrement());
        branches.compute(targetBranch, (key, branch) -> {
            branchCheckBeforeSave(targetBranch, branch, commit);
            recordTables.put(commit.getId(), recordTable);
            return new Branch(targetBranch, commit.getId());
        });
    }

    @Override
    public void addBuild(BuildRecord record) {
        record.setId(NEXT_BUILD_ID.getAndIncrement());
        builds.put(record.getId(), record);
    }

    @Override
    public void updateProjectCommitToBuild(BuildRecord record) {
        BuildRecord r = builds.get(record.getId());
        if (r == null) {
            throw new IllegalArgumentException("could not find build " + record.getId());
        }
        r.setProjectCommitBranch(record.getProjectCommitBranch());
        r.setProjectCommitHash(record.getProjectCommitHash());
        r.setProjectCommitEmail(record.getProjectCommitEmail());
        r.setProjectCommitDetail(record.getProjectCommitDetail());
        r.setProjectCommitDate(record.getProjectCommitDate());
    }

    @Override
    public void finishBuild(BuildRecord record) {
        BuildRecord r = builds.get(record.getId());
        if (r == null) {
            throw new IllegalArgumentException("could not find build " + record.getId());
        }
        r.setFinishedTime(record.getFinishedTime());
        r.setFailed(record.getFailed());
        r.setFailedType(record.getFailedType());
        r.setFailedMessage(record.getFailedMessage());
    }

    @Override
    public BuildRecord getBuild(long buildId) {
        return builds.get(buildId);
    }

    @Override
    public BuildRecord getJobLastBuild(String jobName) {
        long buildId = 0;
        for (BuildRecord record : builds.values()) {
            if (Objects.equals(record.getJobName(), jobName) && record.getFinishedTime() != null) {
                if (record.getId() > buildId) {
                    buildId = record.getId();
                }
            }
        }
        if (buildId > 0) {
            return builds.get(buildId);
        }
        return null;
    }

    @Override
    public Page<BuildRecord> queryBuilds(int pageIndex,
                                         int pageSize,
                                         String jobName,
                                         String envName,
                                         String projectName,
                                         Boolean finished,
                                         Boolean failed,
                                         String failedType) {
        List<BuildRecord> list = new ArrayList<>();
        for (BuildRecord record : builds.values()) {
            if (jobName != null && !jobName.equals(record.getJobName()) ||
                envName != null && !envName.equals(record.getEnvName()) ||
                projectName != null && !projectName.equals(record.getProjectName()) ||
                finished != null && finished == (record.getFinishedTime() == null) ||
                failed != null && !Objects.equals(failed, record.getFailed()) ||
                failedType != null && !failedType.equals(record.getFailedType())) {
                continue;
            }

            list.add(record);
        }

        list.sort(Comparator.comparingLong(BuildRecord::getId));
        List<BuildRecord> data = new ReverseList<>(list).subList(pageIndex * pageIndex, (pageIndex + 1) * pageSize);
        return new Page<>(pageIndex, pageSize, list.size(), data);
    }

    /**
     * get the snapshot of the config, lock is ignored
     *
     */
    private Config getConfig0(String envName, String projectName, String namespace, String key) {
        for (Config config : configs.values()) {
            if (Objects.equals(config.getEnvName(), envName) &&
                Objects.equals(config.getProjectName(), projectName) &&
                Objects.equals(config.getNamespace(), namespace) &&
                Objects.equals(config.getKey(), key)) {
                return config;
            }
        }
        return null;
    }

    /**
     * get the snapshot of the config, lock is ignored
     *
     */
    @Override
    protected Config getConfig(String envName, String projectName, String namespace, String key) {
        synchronized (configs) {
            return getConfig0(envName, projectName, namespace, key);
        }
    }

    /**
     * create the config if not exists first, then get the snapshot of the config
     *
     */
    @Override
    protected Config getOrCreateConfig(String envName, String projectName, String namespace, String key) {
        synchronized (configs) {
            Config config = getConfig0(envName, projectName, namespace, key);
            if (config != null) {
                return config;
            }

            Config newConfig = new Config();
            newConfig.setId(configs.size() + 1);
            newConfig.setEnvName(envName);
            newConfig.setProjectName(projectName);
            newConfig.setNamespace(namespace);
            newConfig.setKey(key);
            configs.put(newConfig.getId(), newConfig);

            return newConfig;
        }
    }

    /**
     * set the config.lockedBy to locker if config.lockedBy = null or config.lockedBy = locker and return true,
     * otherwise return false, meaning locking failed.
     */
    @Override
    protected boolean tryLockConfig(long configId, String locker) {
        synchronized (configs) {
            Config config = configs.get(configId);
            if (config == null) {
                throw new IllegalArgumentException("config id " + configId + " does not exist");
            }
            if (config.getLockedBy() == null) {
                config.setLockedBy(locker);
                config.setLockedTime(new Date());
                return true;
            } else {
                return config.getLockedBy().equals(locker);
            }
        }
    }

    /**
     * get the value of the config, ignoring lock
     */
    @Override
    protected String getConfigValue(long configId) {
        synchronized (configs) {
            Config config = configs.get(configId);
            if (config == null) {
                throw new IllegalArgumentException("config id " + configId + " does not exist");
            }
            return config.getValue();
        }
    }

    /**
     * set the value and clear the lockedBy & lockedTime
     */
    @Override
    protected void setConfigValueAndUnlock(long configId, String value) {
        synchronized (configs) {
            Config config = configs.get(configId);
            if (config == null) {
                throw new IllegalArgumentException("config id " + configId + " does not exist");
            }
            config.setValue(value);
            config.setLockedBy(null);
            config.setLockedTime(null);
        }
    }
}
