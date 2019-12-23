package com.yit.deploy.core.model;

/**
 * Created by nick on 11/09/2017.
 */
public class MysqlDBConnection extends DBConnection {
    public String getDbInstanceId() {
        if (super.getDbInstanceId() == null) {
            return getHost().replace(".mysql.rds.aliyuncs.com", "");
        } else {
            return super.getDbInstanceId();
        }
    }
}
