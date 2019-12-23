package com.yit.deploy.core.parameters.inventory;

import com.yit.deploy.core.algorithm.Graph;
import com.yit.deploy.core.diff.DetailDiff;
import com.yit.deploy.core.diff.Datum;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.JsonSupport;

import java.util.*;

@DetailDiff
public class DeployPlan implements JsonSupport<DeployPlan>, Datum {
    private String id;
    private boolean confirmBeforeFinish;
    @NeedVerify
    private boolean reverseOrder;
    @NeedVerify
    private List<DeployItem> items = new ArrayList<>();
    private String description;
    private int parallel;
    private String syncMessage;
    private String verifyMessage;
    /**
     * used to force refresh, only the change of this field is concerned
     */
    @NeedVerify
    private int requestRefresh;
    @NeedVerify
    private List<String> extraTasksToSkip;

    public void initialize(Environment environment) {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        for (DeployItem item : items) {
            item.initialize(environment, this);
        }
    }

    public DeployItem createSearchItem(Environment environment, String searchScript) {
        DeployItem item = new DeployItem();
        item.setSearchScript(searchScript);
        item.initialize(environment, this);
        return item;
    }

    public void prepareForDeploy(Environment environment) {
        for (int i = 0; i < items.size();) {
            DeployItem item = items.get(i);
            List<DeployItem> children = item.expand(environment, this);
            items.remove(i);
            for (DeployItem child : children) {
                if (!containsItem(child.getProjectName())) {
                    items.add(i, child);
                    i++;
                }
            }
        }
        for (DeployItem item : items) {
            item.prepareForDeploy(this);
        }
    }

    public boolean containsItem(String projectName) {
        return findItem(projectName) != null;
    }

    public DeployItem findItem(String projectName) {
        if (projectName == null) {
            throw new IllegalArgumentException("projectName");
        }
        for (DeployItem item : items) {
            if (projectName.equals(item.getProjectName())) {
                return item;
            }
        }
        return null;
    }

    public void sortItems() {

        Graph<DeployItem, String> graph = new Graph<>();
        for (DeployItem item : items) {
            if (!Lambda.isNullOrEmpty(item.getProjectName())) {
                graph.node(item.getProjectName(), item);
                for (String dependency : item.getDependencies()) {
                    graph.arc(dependency, item.getProjectName());
                }
            }
        }

        List<String> seq = graph.topology();

        items.sort((a ,b) -> {
            int r = Integer.compare(a.getProject().getJobOrder(), b.getProject().getJobOrder());
            if (r != 0) return r;

            if (a.getProjectName() == null) {
                return b.getProjectName() == null ? 0 : 1;
            } else if (b.getProjectName() == null) {
                return -1;
            }

            return Integer.compare(seq.indexOf(a.getProjectName()), seq.indexOf(b.getProjectName()));
        });

        if (reverseOrder) {
            Collections.reverse(items);
        }
    }

    public void verifyForDeploy(int planId) {

        for (DeployItem item : items) {
            item.verifyForDeploy(planId);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isConfirmBeforeFinish() {
        return confirmBeforeFinish;
    }

    public void setConfirmBeforeFinish(boolean confirmBeforeFinish) {
        this.confirmBeforeFinish = confirmBeforeFinish;
    }

    public boolean isReverseOrder() {
        return reverseOrder;
    }

    public void setReverseOrder(boolean reverseOrder) {
        this.reverseOrder = reverseOrder;
    }

    public List<DeployItem> getItems() {
        return items;
    }

    public void setItems(List<DeployItem> items) {
        this.items = items;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getParallel() {
        return parallel;
    }

    public void setParallel(int parallel) {
        this.parallel = parallel;
    }

    public String getSyncMessage() {
        return syncMessage;
    }

    public void setSyncMessage(String syncMessage) {
        this.syncMessage = syncMessage;
    }

    public String getVerifyMessage() {
        return verifyMessage;
    }

    public void setVerifyMessage(String verifyMessage) {
        this.verifyMessage = verifyMessage;
    }

    public int getRequestRefresh() {
        return requestRefresh;
    }

    public void setRequestRefresh(int requestRefresh) {
        this.requestRefresh = requestRefresh;
    }

    public List<String> getExtraTasksToSkip() {
        return extraTasksToSkip;
    }

    public void setExtraTasksToSkip(List<String> extraTasksToSkip) {
        this.extraTasksToSkip = extraTasksToSkip;
    }
}
