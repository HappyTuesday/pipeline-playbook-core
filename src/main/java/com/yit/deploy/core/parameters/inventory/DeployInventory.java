package com.yit.deploy.core.parameters.inventory;

import com.yit.deploy.core.diff.Change;
import com.yit.deploy.core.diff.DetailDiff;
import com.yit.deploy.core.diff.Difference;
import com.yit.deploy.core.diff.ExcludeDiff;
import com.yit.deploy.core.model.JsonSupport;

import java.io.Serializable;
import java.util.*;

import com.yit.deploy.core.utils.Utils;

// DeployInventory must implement Serializable for Jenkins reasons
@DetailDiff
public class DeployInventory implements JsonSupport<DeployInventory>, Serializable {
    private String envName = "";
    private String name = "default";
    private boolean ignoreFailure;
    private boolean autoAdjustOrder;
    private boolean confirmBeforeExecute = false;
    private int retries = 10;
    private boolean confirmBeforeRetry = true;
    @ExcludeDiff
    private String updateDate = Utils.formatDate(new Date());
    @NeedVerify
    private List<DeployPlan> plans = new ArrayList<>();
    private boolean noInterrupt = false;
    private boolean autoAdjustBranch;
    private boolean showUsageDescription = false;
    private String owner;
    private String usageDescription;
    private String notificationMails;

    /**
     * if this field is set to a none-null value, then the whole deploy-inventory will be treated as a shared inventory.
     */
    private String sharedBy;
    /**
     * if sharedBy is not null, then the version field must be ensured to be increased one by one at every time saving
     * this inventory
     */
    @ExcludeDiff
    private long version = 1;
    @ExcludeDiff
    private LinkedList<InventoryChange> changes;

    public DeployInventory() {}

    public DeployInventory(String name) {
        this.name = name;
    }

    public void initialize(Environment environment) {
        if (envName == null || envName.isEmpty()) {
            envName = environment.getName();
        }
        if (plans == null) {
            plans = new ArrayList<>();
        }
        if (plans.isEmpty()) {
            DeployPlan plan = new DeployPlan();
            plan.setId(UUID.randomUUID().toString());
            plans.add(plan);
        }
        for (DeployPlan plan : plans) {
            plan.initialize(environment);
        }

        if (changes == null) {
            changes = new LinkedList<>();
        }

        // clear history dirty data
        changes.removeIf(change -> {
            if (change.getDetails() == null) return true;
            change.getDetails().removeIf(c -> c.getPath() == null || c.getType() == null);
            return change.getDetails().isEmpty();
        });
    }

    public void prepareForDeploy(Environment environment) {
        for (DeployPlan plan : plans) {
            plan.prepareForDeploy(environment);
        }
    }

    public boolean recordChange(DeployInventory old, String user, String changeId) {
        changes.clear();
        changes.addAll(old.changes);

        InventoryChange change = new InventoryChange();
        change.setId(changeId);
        change.setTime(Utils.formatDate(new Date()));
        change.setUser(user);
        if (old.changes.isEmpty()) {
            change.getDetails().add(Change.changed("", "{}"));
        }

        Difference diff = new Difference(true);

        change.getDetails().addAll(diff.diff(old, this));
        if (change.getDetails().isEmpty()) {
            return false;
        }
        changes.addFirst(change);

        while (changes.size() > 30) {
            changes.removeLast();
        }

        return true;
    }

    public InventoryChange getChangesFroVerify(DeployInventory from) {

        InventoryChange change = null;
        if (from != null) {
            change = new InventoryChange();
            change.setDetails(new Difference(true, f -> f.getAnnotation(NeedVerify.class) != null).diff(from, this));
        }

        return change;
    }

    public void verifyForDeploy() {
        for (int i = 0; i < plans.size(); i++) {
            plans.get(i).verifyForDeploy(i);
        }
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public boolean isIgnoreFailure() {
        return ignoreFailure;
    }

    public void setIgnoreFailure(boolean ignoreFailure) {
        this.ignoreFailure = ignoreFailure;
    }

    public boolean isAutoAdjustOrder() {
        return autoAdjustOrder;
    }

    public void setAutoAdjustOrder(boolean autoAdjustOrder) {
        this.autoAdjustOrder = autoAdjustOrder;
    }

    public boolean isConfirmBeforeExecute() {
        return confirmBeforeExecute;
    }

    public void setConfirmBeforeExecute(boolean confirmBeforeExecute) {
        this.confirmBeforeExecute = confirmBeforeExecute;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public boolean isConfirmBeforeRetry() {
        return confirmBeforeRetry;
    }

    public void setConfirmBeforeRetry(boolean confirmBeforeRetry) {
        this.confirmBeforeRetry = confirmBeforeRetry;
    }

    public List<DeployPlan> getPlans() {
        return plans;
    }

    public void setPlans(List<DeployPlan> plans) {
        this.plans = plans;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
    }

    public boolean isNoInterrupt() {
        return noInterrupt;
    }

    public void setNoInterrupt(boolean noInterrupt) {
        this.noInterrupt = noInterrupt;
    }

    public boolean isAutoAdjustBranch() {
        return autoAdjustBranch;
    }

    public void setAutoAdjustBranch(boolean autoAdjustBranch) {
        this.autoAdjustBranch = autoAdjustBranch;
    }

    public boolean isShowUsageDescription() {
        return showUsageDescription;
    }

    public void setShowUsageDescription(boolean showUsageDescription) {
        this.showUsageDescription = showUsageDescription;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getUsageDescription() {
        return usageDescription;
    }

    public void setUsageDescription(String usageDescription) {
        this.usageDescription = usageDescription;
    }

    public String getSharedBy() {
        return sharedBy;
    }

    public void setSharedBy(String sharedBy) {
        this.sharedBy = sharedBy;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public LinkedList<InventoryChange> getChanges() {
        return changes;
    }

    public void setChanges(LinkedList<InventoryChange> changes) {
        this.changes = changes;
    }

    public String getNotificationMails() {
        return notificationMails;
    }

    public void setNotificationMails(String notificationMails) {
        this.notificationMails = notificationMails;
    }
}
