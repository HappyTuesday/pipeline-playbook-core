package com.yit.deploy.core.model;

import java.io.Serializable;

/**
 * Created by nick on 11/09/2017.
 */
public class DBConnection implements Serializable,Cloneable {
    private String host;
    private int port;
    private String dbName;
    private String userName;
    private String password;
    private String rootUserName;
    private String rootPassword;
    private String url;
    private String characterSetName;
    private String dbInstanceId;

    @Override
    public String toString() {
        return String.format("%s@%s::%s:%d", userName, dbName, host, port);
    }

    @Override
    public DBConnection clone() throws CloneNotSupportedException {
        return (DBConnection) super.clone();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRootUserName() {
        return rootUserName;
    }

    public void setRootUserName(String rootUserName) {
        this.rootUserName = rootUserName;
    }

    public String getRootPassword() {
        return rootPassword;
    }

    public void setRootPassword(String rootPassword) {
        this.rootPassword = rootPassword;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCharacterSetName() {
        return characterSetName;
    }

    public void setCharacterSetName(String characterSetName) {
        this.characterSetName = characterSetName;
    }

    public String getDbInstanceId() { return dbInstanceId; }

    public void setDbInstanceId(String dbInstanceId) { this.dbInstanceId = dbInstanceId; }
}
