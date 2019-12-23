package com.yit.deploy.core.storage;

import java.util.Objects;

public class StorageConfig {
    public final String url;
    public final String username;
    public final String password;
    public final String defaultBranch;

    public StorageConfig() {
        this.url = null;
        this.username = null;
        this.password = null;
        this.defaultBranch = null;
    }

    public StorageConfig(String url, String username, String password, String defaultBranch) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.defaultBranch = defaultBranch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, username, password, defaultBranch);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StorageConfig) {
            StorageConfig sc = (StorageConfig) obj;
            return Objects.equals(url, sc.url) &&
                Objects.equals(username, sc.username) &&
                Objects.equals(password, sc.password) &&
                Objects.equals(defaultBranch, sc.defaultBranch);
        }
        return false;
    }
}
