package com.yit.deploy.core.storage.persistent;

import com.yit.deploy.core.records.*;
import com.yit.deploy.core.storage.DeployStorage;
import com.yit.deploy.core.storage.StorageConfig;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.*;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class PersistentDeployStorage extends DeployStorage {

    private final SqlSessionFactory factory;

    public PersistentDeployStorage(StorageConfig config) {
        super(config);

        Properties properties = new Properties();
        properties.setProperty("db.url", config.url);
        properties.setProperty("db.username", config.username);
        properties.setProperty("db.password", config.password);

        SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        try {
            this.factory = builder.build(Resources.getResourceAsReader("storage/persistent/mybatis-config.xml"),
                properties);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Branch> loadBranches() {
        try (SqlSession session = factory.openSession()) {
            RecordTableMapper mapper = session.getMapper(RecordTableMapper.class);
            return mapper.getBranches();
        }
    }

    @Override
    public Branch loadBranch(String branchName) {
        try (SqlSession session = factory.openSession()) {
            RecordTableMapper mapper = session.getMapper(RecordTableMapper.class);
            return mapper.getBranch(branchName);
        }
    }

    @Override
    protected DeployRecordTable loadCommitDetail(long id) {
        try (SqlSession session = factory.openSession()) {
            RecordTableMapper mapper = session.getMapper(RecordTableMapper.class);
            Commit c = mapper.getCommit(id);
            if (c == null) {
                throw new IllegalArgumentException("could not find commit " + id);
            }
            DeployRecordTable table = new DeployRecordTable();
            table.setCommit(c);
            table.setEnvs(mapper.getEnvironments(id));
            if (table.getEnvs().isEmpty()) {
                table.setEnvs(null);
            }
            table.setHosts(mapper.getHosts(id));
            if (table.getHosts().isEmpty()) {
                table.setHosts(null);
            }
            table.setHostGroups(mapper.getHostGroups(id));
            if (table.getHostGroups().isEmpty()) {
                table.setHostGroups(null);
            }
            table.setProjects(mapper.getProjects(id));
            if (table.getProjects().isEmpty()) {
                table.setProjects(null);
            }
            table.setAssigns(mapper.getAssigns(id));
            if (table.getAssigns().isEmpty()) {
                table.setAssigns(null);
            }
            return table;
        }
    }

    @Override
    public void saveCommitDetail(String targetBranch, DeployRecordTable recordTable) {
        try (SqlSession session = factory.openSession()) {
            RecordTableMapper mapper = session.getMapper(RecordTableMapper.class);
            Branch branch = mapper.getBranch(targetBranch);
            Commit commit = recordTable.getCommit();
            commit.setTimestamp(new Date());
            if (commit.getParentId() != null && commit.getParentId() == 0) {
                commit.setParentId(null);
            }
            branchCheckBeforeSave(targetBranch, branch, commit);

            mapper.addCommit(commit);
            long commitId = commit.getId();
            if (branch == null) {
                mapper.addBranch(new Branch(targetBranch, commitId));
            } else {
                branch.setHead(commitId);
                mapper.updateBranch(branch);
            }

            if (recordTable.getEnvs() != null) {
                for (EnvironmentRecord r : recordTable.getEnvs()) {
                    r.setCommitId(commitId);
                    mapper.addEnvironment(r);
                }
            }

            if (recordTable.getHosts() != null) {
                for (HostRecord r : recordTable.getHosts()) {
                    r.setCommitId(commitId);
                    mapper.addHost(r);
                }
            }

            if (recordTable.getHostGroups() != null) {
                for (HostGroupRecord r : recordTable.getHostGroups()) {
                    r.setCommitId(commitId);
                    mapper.addHostGroup(r);
                }
            }

            if (recordTable.getProjects() != null) {
                for (ProjectRecord r : recordTable.getProjects()) {
                    r.setCommitId(commitId);
                    mapper.addProject(r);
                }
            }

            if (recordTable.getAssigns() != null) {
                for (Assignment r : recordTable.getAssigns()) {
                    r.setCommitId(commitId);
                    mapper.addAssign(r);
                }
            }

            session.commit();
        }
    }

    @Override
    public void addBuild(BuildRecord record) {
        try (SqlSession session = factory.openSession()) {
            BuildMapper mapper = session.getMapper(BuildMapper.class);
            mapper.addBuild(record);
            session.commit();
        }
    }

    @Override
    public void updateProjectCommitToBuild(BuildRecord record) {
        try (SqlSession session = factory.openSession()) {
            BuildMapper mapper = session.getMapper(BuildMapper.class);
            mapper.updateProjectCommitToBuild(record);
            session.commit();
        }
    }

    @Override
    public void finishBuild(BuildRecord record) {
        try (SqlSession session = factory.openSession()) {
            BuildMapper mapper = session.getMapper(BuildMapper.class);
            mapper.finishBuild(record);
            session.commit();
        }
    }

    @Override
    public BuildRecord getBuild(long buildId) {
        try (SqlSession session = factory.openSession()) {
            BuildMapper mapper = session.getMapper(BuildMapper.class);
            return mapper.getBuild(buildId);
        }
    }

    @Override
    public BuildRecord getJobLastBuild(String jobName) {
        try (SqlSession session = factory.openSession()) {
            BuildMapper mapper = session.getMapper(BuildMapper.class);
            return mapper.getJobLastBuild(jobName);
        }
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
        try (SqlSession session = factory.openSession()) {
            BuildMapper mapper = session.getMapper(BuildMapper.class);
            long totalCount = mapper.queryBuildsCount(jobName, envName, projectName, finished, failed, failedType);
            long from = pageIndex * pageSize;
            long to = (pageIndex + 1) * pageSize - 1;
            List<BuildRecord> data = mapper.queryBuilds(from, to, jobName, envName, projectName,
                finished, failed, failedType);

            return new Page<>(pageIndex, pageSize, totalCount, data);
        }
    }

    /**
     * get the snapshot of the config, lock is ignored
     */
    @Override
    protected Config getConfig(String envName, String projectName, String namespace, String key) {
        try (SqlSession session = factory.openSession()) {
            ConfigMapper mapper = session.getMapper(ConfigMapper.class);
            return mapper.getConfig(envName, projectName, namespace, key);
        }
    }

    /**
     * create the config if not exists first, then get the snapshot of the config
     */
    @Override
    protected Config getOrCreateConfig(String envName, String projectName, String namespace, String key) {
        try (SqlSession session = factory.openSession(TransactionIsolationLevel.REPEATABLE_READ)) {
            ConfigMapper mapper = session.getMapper(ConfigMapper.class);
            Config config = mapper.getConfig(envName, projectName, namespace, key);
            if (config != null) {
                return config;
            }

            Config newConfig = new Config();
            newConfig.setEnvName(envName);
            newConfig.setProjectName(projectName);
            newConfig.setNamespace(namespace);
            newConfig.setKey(key);

            mapper.addConfig(newConfig);
            session.commit();
            return newConfig;
        }
    }

    /**
     * set the config.lockedBy to locker if config.lockedBy = null or config.lockedBy = locker and return true,
     * otherwise return false, meaning locking failed.
     */
    @Override
    protected boolean tryLockConfig(long configId, String locker) {
        try (SqlSession session = factory.openSession(TransactionIsolationLevel.REPEATABLE_READ)) {
            ConfigMapper mapper = session.getMapper(ConfigMapper.class);
            String lockedBy = mapper.getLockedBy(configId);

            if (lockedBy != null) {
                return lockedBy.equals(locker);
            }

            mapper.updateLockInfo(configId, locker, new Date());
            session.commit();
            return true;
        }
    }

    /**
     * get the value of the config, ignoring lock
     */
    @Override
    protected String getConfigValue(long configId) {
        try (SqlSession session = factory.openSession()) {
            ConfigMapper mapper = session.getMapper(ConfigMapper.class);
            return mapper.getConfigValue(configId);
        }
    }

    /**
     * set the value and clear the lockedBy & lockedTime
     */
    @Override
    protected void setConfigValueAndUnlock(long configId, String value) {
        try (SqlSession session = factory.openSession()) {
            ConfigMapper mapper = session.getMapper(ConfigMapper.class);
            mapper.setConfigValue(configId, value);
            mapper.updateLockInfo(configId, null, null);
            session.commit();
        }
    }
}
