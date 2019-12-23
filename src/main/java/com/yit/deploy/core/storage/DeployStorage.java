package com.yit.deploy.core.storage;

import com.yit.deploy.core.exceptions.BackoffTimeoutException;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.records.*;
import com.yit.deploy.core.storage.persistent.PersistentDeployStorage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;

public abstract class DeployStorage {

    private static Logger logger = Logger.getLogger(DeployStorage.class.getName());

    private static DeployStorage instance;

    public static synchronized DeployStorage getInstance(StorageConfig storageConfig) {
        if (instance == null ||
            !instance.storageConfig.equals(storageConfig)) {

            if (storageConfig.url == null || storageConfig.url.isEmpty()) {
                instance = new LocalDeployStorage(storageConfig);
            } else {
                instance = new PersistentDeployStorage(storageConfig);
            }
        }
        return instance;
    }

    private final StorageConfig storageConfig;

    private final Map<Long, DeployRecordTable> commits = new ConcurrentHashMap<>();

    protected DeployStorage(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;
    }

    public abstract List<Branch> loadBranches();

    public abstract Branch loadBranch(String branchName);

    protected abstract DeployRecordTable loadCommitDetail(long id);

    protected abstract void saveCommitDetail(String targetBranch, DeployRecordTable recordTable);

    public abstract void addBuild(BuildRecord record);

    public abstract void updateProjectCommitToBuild(BuildRecord record);

    public abstract void finishBuild(BuildRecord record);

    public abstract BuildRecord getBuild(long buildId);

    public abstract BuildRecord getJobLastBuild(String jobName);

    public abstract Page<BuildRecord> queryBuilds(int pageIndex,
                                                  int pageSize,
                                                  String jobName,
                                                  String envName,
                                                  String projectName,
                                                  Boolean finished,
                                                  Boolean failed,
                                                  String failedType);

    public List<DeployRecordTable> getLatestCommits(long from) {
        return getLatestCommits(from, this.storageConfig.defaultBranch);
    }

    /**
     * get all commits from 'from' (exclude) to branch (include)
     * @param start
     * @param branchName
     * @return
     */
    public List<DeployRecordTable> getLatestCommits(long start, String branchName) {
        if (branchName == null || branchName.isEmpty()) {
            branchName = storageConfig.defaultBranch;
        }
        Branch branch = loadBranch(branchName);
        if (branch == null) {
            return Collections.emptyList();
        }

        return getCommits(branch.getHead(), start);
    }

    public List<DeployRecordTable> getCommits(long from, long start) {
        LinkedList<DeployRecordTable> list = new LinkedList<>();

        for (long id = from; id != start;) {
            DeployRecordTable detail = commits.get(id);
            if (detail == null) {
                // in some cases, same commit detail may be executed more than once,
                // but is OK, deal to the readonly behavior of the commits
                commits.put(id, detail = loadCommitDetail(id));
            }
            if (detail == null) {
                throw new IllegalConfigException("invalid commit id " + id);
            }

            list.addFirst(detail);

            Long parentId = detail.getCommit().getParentId();
            if (parentId == null) {
                if (start == 0) {
                    break;
                } else {
                    throw new IllegalConfigException("invalid commit from id " + start);
                }
            }

            id = parentId;
        }

        return list;
    }

    public List<DeployRecordTable> loadCommits(long from, int count) {
        LinkedList<DeployRecordTable> list = new LinkedList<>();

        for (long id = from; count-- > 0;) {
            DeployRecordTable detail = commits.get(id);
            if (detail == null) {
                // in some cases, same commit detail may be executed more than once,
                // but is OK, deal to the readonly behavior of the commits
                commits.put(id, detail = loadCommitDetail(id));
            }
            if (detail == null) {
                throw new IllegalConfigException("invalid commit id " + id);
            }

            list.addFirst(detail);

            Long parentId = detail.getCommit().getParentId();
            if (parentId == null) {
                break;
            }

            id = parentId;
        }

        return list;
    }

    public void appendCommitDetail(String targetBranch, DeployRecordTable recordTable) {
        this.saveCommitDetail(targetBranch, recordTable);
        commits.put(recordTable.getCommit().getId(), recordTable);
    }

    protected void branchCheckBeforeSave(String targetBranch, Branch branch, Commit commit) {
        long baseCommitId = commit.getParentId() == null ? 0 : commit.getParentId();
        if (branch != null && (baseCommitId == 0 || commit.getParentId() != branch.getHead())) {
            throw new IllegalConfigException("commit id does not match. the head of " + targetBranch + " is " + branch.getHead());
        }
    }

    public String getConfigValue(String envName, String projectName, String namespace, String key, String locker) {
        int no = 0;
        do {
            Config config = getConfig(envName, projectName, namespace, key);

            if (config == null) {
                return null;
            }

            if (config.getLockedBy() == null || config.getLockedBy().equals(locker)) {
                return config.getValue();
            }

            backoffWaitForConfig(envName, projectName, namespace, key, no++);
        } while (true);
    }

    public void updateConfigValue(String envName, String projectName, String namespace, String key, String locker, Function<String, String> f) {
        Config config = getOrCreateConfig(envName, projectName, namespace, key);

        String oldValue;
        if (config.getLockedBy() != null && config.getLockedBy().equals(locker)) {
            oldValue = f.apply(config.getValue());
        } else {
            int no = 0;
            while (!tryLockConfig(config.getId(), locker)) {
                backoffWaitForConfig(envName, projectName, namespace, key, no++);
            }
            oldValue = getConfigValue(config.getId());
        }

        String newValue = oldValue;
        try {
            newValue = f.apply(oldValue);
        } finally {
            // 如果获取值失败，config会设置为原来的值
            // 目的为了在任何情况下都能解锁
            setConfigValueAndUnlock(config.getId(), newValue);
        }
    }

    /**
     * get the snapshot of the config, lock is ignored
     */
    protected abstract Config getConfig(String envName, String projectName, String namespace, String key);

    /**
     * create the config if not exists first, then get the snapshot of the config
     */
    protected abstract Config getOrCreateConfig(String envName, String projectName, String namespace, String key);

    /**
     * set the config.lockedBy to locker if config.lockedBy = null or config.lockedBy = locker and return true,
     * otherwise return false, meaning locking failed.
     */
    protected abstract boolean tryLockConfig(long configId, String locker);

    /**
     * get the value of the config, ignoring lock
     */
    protected abstract String getConfigValue(long configId);

    /**
     * set the value and clear the lockedBy & lockedTime
     */
    protected abstract void setConfigValueAndUnlock(long configId, String value);

    private void backoffWaitForConfig(String envName, String projectName, String namespace, String key, int no) {
        if (no < 0) {
            throw new IllegalArgumentException(String.valueOf(no));
        }

        if (no > 100) {
            throw new BackoffTimeoutException(String.format("backoff wait for config lock " +
                    "[env = %s, project = %s] %s.%s timeout after %d times waiting",
                envName, projectName, namespace, key, no));
        }

        if (no > 12) {
            logger.warning(String.format("backoff wait for config lock [env = %s, project = %s] %s.%s after %d times",
                envName, projectName, namespace, key, no));
            no = 12;
        }

        int ms = (1 << no) * 10;

        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
