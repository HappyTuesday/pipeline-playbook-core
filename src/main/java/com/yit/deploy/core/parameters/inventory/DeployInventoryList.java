package com.yit.deploy.core.parameters.inventory;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

public class DeployInventoryList extends ArrayList<DeployInventory> {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void initialize(Environment environment) {
        for (DeployInventory inventory : this) {
            inventory.initialize(environment);
        }
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static DeployInventoryList fromJson(String json) {
        if (Strings.isNullOrEmpty(json)) {
            return new DeployInventoryList();
        } else {
            return gson.fromJson(json, DeployInventoryList.class);
        }
    }
}
