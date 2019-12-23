package com.yit.deploy.core.storage.persistent;

import com.yit.deploy.core.records.DeployRecordTable;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class LinkedHashMapTypeHandler extends BaseTypeHandler<LinkedHashMap> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LinkedHashMap parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, DeployRecordTable.GSON.toJson(parameter));
    }

    private static LinkedHashMap fromJson(String s) {
        return s == null ? null : DeployRecordTable.GSON.fromJson(s, LinkedHashMap.class);
    }

    @Override
    public LinkedHashMap getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return fromJson(rs.getString(columnName));
    }

    @Override
    public LinkedHashMap getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return fromJson(rs.getString(columnIndex));
    }

    @Override
    public LinkedHashMap getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return fromJson(cs.getString(columnIndex));
    }
}
