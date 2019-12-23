package com.yit.deploy.core.parameters.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.yit.deploy.core.diff.Change;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.JsonSupport;
import com.yit.deploy.core.parameters.inventory.InventoryChangeDetail.ChangeType;

/**
 * Created by nick on 17/11/2017.
 */
public class InventoryChange implements JsonSupport<InventoryChange> {
    /**
     * this should be unique if not null
     */
    private String id;
    private String user;
    private String time;
    private List<Change> details = new ArrayList<>();

    public List<Change> findChanges(String pattern, String ... excludes) {
        return Lambda.findAll(details, c -> c.match(pattern) && !Lambda.any(excludes, c::match));
    }

    public boolean hasChanges(String folder, String ... excludedProperties) {
        String pattern = folder + "/**";
        List<String> excludes = Lambda.map(Arrays.asList(excludedProperties), p -> folder + "/" + p);
        return Lambda.any(details, c -> c.match(pattern) && !Lambda.any(excludes, c::match));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public List<Change> getDetails() {
        return details;
    }

    public void setDetails(List<Change> details) {
        this.details = details;
    }
}
