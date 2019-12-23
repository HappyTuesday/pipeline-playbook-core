package com.yit.deploy.core.records;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yit.deploy.core.function.FieldGetter;
import com.yit.deploy.core.function.FieldSetter;
import com.yit.deploy.core.function.Lambda;

import java.io.Reader;
import java.util.*;
import java.util.function.BiFunction;

public class DeployRecordTable {

    public static final Gson GSON = new GsonBuilder().create();

    private Commit commit;
    private List<EnvironmentRecord> envs;
    private List<ProjectRecord> projects;
    private List<Assignment> assigns;
    private List<HostRecord> hosts;
    private List<HostGroupRecord> hostGroups;

    private <T> void mergeListField(DeployRecordTable table1,
                                DeployRecordTable table2,
                                FieldGetter<DeployRecordTable, List<T>> fieldGetter,
                                FieldSetter<DeployRecordTable, List<T>> fieldSetter,
                                BiFunction<T, T, Boolean> equals
    ) {
        List<T> field1 = fieldGetter.get(table1);
        List<T> field2 = fieldGetter.get(table2);
        List<T> field;

        if (field1 == null || field1.isEmpty()) {
            field = field2;
        } else if (field2 == null || field2.isEmpty()) {
            field = field1;
        } else {
            field = new ArrayList<>(field1.size() + field2.size());
            for (T t : field1) {
                if (Lambda.all(field2, x -> !equals.apply(t, x))) { // t is not contained in table2
                    field.add(t);
                }
                field.addAll(field2); // all values existing both in table1 and table2 are only inserted here
            }
        }
        fieldSetter.set(this, field);
    }

    public DeployRecordTable plus(DeployRecordTable that) {
        DeployRecordTable table = new DeployRecordTable();

        table.commit = this.commit;

        table.mergeListField(this, that,
            DeployRecordTable::getEnvs,
            DeployRecordTable::setEnvs,
            (x, y) -> Objects.equals(x.getName(), y.getName()));

        table.mergeListField(this, that,
            DeployRecordTable::getHosts,
            DeployRecordTable::setHosts,
            (x, y) -> Objects.equals(x.getName(), y.getName()) && Objects.equals(x.getEnv(), y.getEnv()));

        table.mergeListField(this, that,
            DeployRecordTable::getHostGroups,
            DeployRecordTable::setHostGroups,
            (x, y) -> Objects.equals(x.getName(), y.getName()) && Objects.equals(x.getEnv(), y.getEnv()));

        table.mergeListField(this, that,
            DeployRecordTable::getAssigns,
            DeployRecordTable::setAssigns,
            (x, y) -> Objects.equals(x.getId(), y.getId()));

        table.mergeListField(this, that,
            DeployRecordTable::getProjects,
            DeployRecordTable::setProjects,
            (x, y) -> Objects.equals(x.getProjectName(), y.getProjectName()));

        return table;
    }

    public Commit getCommit() {
        return commit;
    }

    public void setCommit(Commit commit) {
        this.commit = commit;
    }

    public static DeployRecordTable fromJson(String json) {
        return GSON.fromJson(json, DeployRecordTable.class);
    }

    public List<EnvironmentRecord> getEnvs() {
        return envs;
    }

    public void setEnvs(List<EnvironmentRecord> envs) {
        this.envs = envs;
    }

    public List<ProjectRecord> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectRecord> projects) {
        this.projects = projects;
    }

    public List<HostRecord> getHosts() {
        return hosts;
    }

    public void setHosts(List<HostRecord> hosts) {
        this.hosts = hosts;
    }

    public List<HostGroupRecord> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(List<HostGroupRecord> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public List<Assignment> getAssigns() {
        return assigns;
    }

    public void setAssigns(List<Assignment> assigns) {
        this.assigns = assigns;
    }

    public static class SubmitDraftForm {
        private String targetBranch;
        private DeployRecordTable draft;

        public DeployRecordTable getDraft() {
            return draft;
        }

        public void setDraft(DeployRecordTable draft) {
            this.draft = draft;
        }

        public static SubmitDraftForm fromJson(Reader reader) {
            return GSON.fromJson(reader, SubmitDraftForm.class);
        }

        public String getTargetBranch() {
            return targetBranch;
        }

        public void setTargetBranch(String targetBranch) {
            this.targetBranch = targetBranch;
        }
    }

    public static class SubmitDraftResult {
        private String error;
        private Commit commit;

        public String isError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public Commit getCommit() {
            return commit;
        }

        public void setCommit(Commit commit) {
            this.commit = commit;
        }
    }
}
