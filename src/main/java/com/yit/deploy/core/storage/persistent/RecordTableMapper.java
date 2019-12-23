package com.yit.deploy.core.storage.persistent;

import com.yit.deploy.core.records.*;

import java.util.List;

public interface RecordTableMapper {
    Commit getCommit(long id);

    Branch getBranch(String name);

    List<Branch> getBranches();

    List<EnvironmentRecord> getEnvironments(long commitId);

    List<HostRecord> getHosts(long commitId);

    List<HostGroupRecord> getHostGroups(long commitId);

    List<ProjectRecord> getProjects(long commitId);

    List<Assignment> getAssigns(long commitId);

    void addCommit(Commit commit);

    void addBranch(Branch branch);

    void updateBranch(Branch branch);

    void addEnvironment(EnvironmentRecord env);

    void addHost(HostRecord host);

    void addHostGroup(HostGroupRecord hg);

    void addProject(ProjectRecord project);

    void addAssign(Assignment assign);


}
