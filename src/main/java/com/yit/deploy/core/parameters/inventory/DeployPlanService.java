package com.yit.deploy.core.parameters.inventory;

import com.yit.deploy.core.model.DeployResponse;

import java.util.List;

public interface DeployPlanService {
    DeployInventory httpPollDeployInventory(String inventoryName, long requestedVersion, long timeout);

    DeployResponse<List<InventoryChange>> httpSaveDeployInventory(String inventoryJson, String changeId);

    void removeInventory(String inventoryName);

    DeployItem findDeployItem(String projectName, int planIndex, String inventoryName);

    DeployItem findDeployItem(String projectName);

    void updateDeployItem(String projectName, DeployItem deployItem, int planIndex, String inventoryName);

    void updateDeployItem(String projectName, String deployItemJson, int planIndex, String inventoryName);

    void updateDeployItem(String projectName, DeployItem deployItem);

    String getActiveInventoryName();

    void setActiveInventoryName(String name);

    void clearActiveInventoryName();

    DeployInventory getDeployInventory(String inventoryName);

    void saveDeployInventory(DeployInventory inventory);

    void saveDeployInventory(DeployInventory inventory, String changeId);

    void saveDeployInventory(DeployInventory inventory, DeployInventory old);

    void initializeDeployInventory(DeployInventory inventory);

    void prepareDeployInventoryForDeploy(DeployInventory inventory);
}
