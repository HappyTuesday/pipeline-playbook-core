package com.yit.deploy.core.storage.persistent;

import com.yit.deploy.core.records.Config;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface ConfigMapper {
    void addConfig(Config config);

    Config getConfig(@Param("envName") String envName,
                     @Param("projectName") String projectName,
                     @Param("namespace") String namespace,
                     @Param("key") String key);

    String getLockedBy(@Param("id") long id);

    void updateLockInfo(@Param("id") long id,
                        @Param("lockedBy") String lockedBy,
                        @Param("lockedTime") Date lockedTime);

    String getConfigValue(@Param("id") long id);

    void setConfigValue(@Param("id") long id,
                        @Param("value") String value);
}
