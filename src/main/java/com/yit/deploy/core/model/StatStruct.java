package com.yit.deploy.core.model;

import java.io.Serializable;

/**
 * Created by nick on 27/09/2017.
 */
public class StatStruct implements Serializable {
    private boolean exists;
    private String path;
    private FileType type;
    private String mode;
    private String owner;
    private String group;

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isDirectory() {
        return FileType.directory.equals(type);
    }
}