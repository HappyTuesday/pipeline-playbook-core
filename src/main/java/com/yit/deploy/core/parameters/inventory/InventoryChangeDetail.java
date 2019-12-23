package com.yit.deploy.core.parameters.inventory;

import com.yit.deploy.core.model.JsonSupport;

/**
 * Created by nick on 17/11/2017.
 */
public class InventoryChangeDetail implements JsonSupport<InventoryChangeDetail> {
    private ChangeType changeType;
    private String propertyName;
    private Object oldValue;
    private Object value;
    private Integer planIndex;
    private DeployPlan plan;
    private Integer deployItemIndex;
    private DeployItem deployItem;
    private String projectName;
    private String summary;

    public ChangeType getChangeType() {
        return changeType;
    }

    public void makeSummary() {
        switch (changeType) {
            case create:
                summary = "create inventory";
                break;
            case update:
                summary = String.format("change %s from %s to %s", propertyName, oldValue, value);
                break;
            case addPlan:
                summary = String.format("add plan #%d [%s]", planIndex + 1, plan.getDescription());
                break;
            case updatePlan:
                summary = String.format("change %s of plan #%d from %s to %s", propertyName, planIndex + 1, oldValue, value);
                break;
            case deletePlan:
                summary = String.format("delete plan #%d [%s]", planIndex + 1, plan.getDescription());
                break;
            case addDeployItem:
                summary = String.format("add deploy item #%d [%s] in plan #%d", deployItemIndex + 1, deployItem.getProjectName(), planIndex + 1);
                break;
            case updateDeployItem:
                summary = String.format("change %s of deploy item #%d [%s] from %s to %s in plan #%d", propertyName, deployItemIndex + 1, projectName, oldValue, value, planIndex + 1);
                break;
            case deleteDeployItem:
                summary = String.format("delete deploy item #%d [%s] in plan #%d", deployItemIndex + 1, deployItem.getProjectName(), planIndex + 1);
                break;
            case updateDeployItemParameter:
                summary = String.format("change parameter %s of deploy item #%d [%s] from %s to %s in plan #%d", propertyName, deployItemIndex + 1, projectName, oldValue, value, planIndex + 1);
                break;
        }
    }

    public boolean relativeToDeployItem(String projectName) {
        if (projectName == null || projectName.isEmpty()) {
            throw new IllegalArgumentException("projectName");
        }

        if (projectName.equals(this.projectName)) return true;
        if ((ChangeType.addPlan.equals(changeType) || ChangeType.deletePlan.equals(changeType)) && plan != null) {
            for (DeployItem item : plan.getItems()) {
                if (projectName.equals(item.getProjectName())) return true;
            }
        }
        if (ChangeType.create.equals(changeType)) {
            return true;
        }
        return false;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Integer getPlanIndex() {
        return planIndex;
    }

    public void setPlanIndex(Integer planIndex) {
        this.planIndex = planIndex;
    }

    public DeployPlan getPlan() {
        return plan;
    }

    public void setPlan(DeployPlan plan) {
        this.plan = plan;
    }

    public Integer getDeployItemIndex() {
        return deployItemIndex;
    }

    public DeployItem getDeployItem() {
        return deployItem;
    }

    public void setDeployItem(DeployItem deployItem) {
        this.deployItem = deployItem;
    }

    public void setDeployItemIndex(int deployItemIndex) {
        this.deployItemIndex = deployItemIndex;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public enum ChangeType {
        /**
         * create the deploy inventory
         */
        create,
        /**
         * change the properties of the inventory, excluding the 'plans' property
         */
        update,
        /**
         * add plan
         */
        addPlan,
        /**
         * delete plan
         */
        deletePlan,
        /**
         * update plan property, excluding the 'items' property of the 'plans' property of the inventory
         */
        updatePlan,
        /**
         * add deploy item
         */
        addDeployItem,
        /**
         * delete deploy item
         */
        deleteDeployItem,
        /**
         * update property of deploy item, excluding the 'parameters' property the the deploy item
         */
        updateDeployItem,
        /**
         * add / delete / update value of parameter of deploy item
         */
        updateDeployItemParameter
    }
}