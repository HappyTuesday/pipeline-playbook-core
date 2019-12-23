package com.yit.deploy.core.records;

import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.variables.variable.Variable;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Assignment implements Record {
    private long commitId;
    private String id;
    private String envName;
    private String projectName;
    private AssignmentScope scope;
    private VariableInfo variableInfo;
    private boolean disabled;

    public long getCommitId() {
        return commitId;
    }

    public void setCommitId(long commitId) {
        this.commitId = commitId;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getEnvName() {
        return envName;
    }

    public AssignmentScope getScope() {
        return scope;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setScope(AssignmentScope scope) {
        this.scope = scope;
    }

    public VariableInfo getVariableInfo() {
        variableInfo.setId(this.id);
        return variableInfo;
    }

    public void setVariableInfo(VariableInfo variableInfo) {
        this.variableInfo = variableInfo;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    private static int compareId(String id1, String id2) {
        int i = 0, j = 0;
        while (true) {
            int k = id1.indexOf('.', i), l = id2.indexOf('.', j);

            String s = k < 0 ? id1.substring(i) : id1.substring(i, k);
            String t = l < 0 ? id2.substring(j) : id2.substring(j, l);

            int x = Integer.parseInt(s), y = Integer.parseInt(t);
            if (x < y) return -1;
            if (x > y) return 1;
            if (k < 0 && l < 0) return 0;
            if (k < 0) return -1;
            if (l < 0) return 1;

            i = k + 1;
            j = l + 1;
        }
    }

    /**
     * insert an assignment record to an assignment list,
     * by the id order defined in compareId.
     * we assure that all assignments with id defined are at the end of the list
     * and are already sorted by the compareId.
     * @param list
     */
    public void insertToVariableInfoList(List<VariableInfo> list) {
        if (this.disabled) {
            // disabled records only used to remove all previous records with the same id
            list.removeIf(info -> this.id.equals(info.getId()));
            return;
        }

        VariableInfo self = new VariableInfo(this.variableInfo);
        self.setId(this.id);
        for (int i = list.size() - 1; i >= 0; i--) {
            VariableInfo info = list.get(i);
            if (info.getId() == null) {
                list.add(i + 1, self);
                return;
            }
            int compare = Assignment.compareId(info.getId(), this.id);
            if (compare == 0) {
                list.set(i, self);
                return;
            }
            if (compare < 0) { // we got the first one who smaller than us
                list.add(i + 1, self);
                return;
            }
        }
        // insert to the beginning of the list, since no one is smaller than us
        list.add(0, self);
    }
}
