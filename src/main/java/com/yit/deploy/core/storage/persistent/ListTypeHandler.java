package com.yit.deploy.core.storage.persistent;

import com.google.gson.reflect.TypeToken;
import com.yit.deploy.core.records.DeployRecordTable;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractCollection;
import java.util.List;

public class ListTypeHandler extends BaseTypeHandler<List> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, DeployRecordTable.GSON.toJson(parameter));
    }

    private static List fromJson(String s) {
        return s == null ? null : DeployRecordTable.GSON.fromJson(s, new TypeToken<List>(){}.getType());
    }

    @Override
    public List getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return fromJson(rs.getString(columnName));
    }

    @Override
    public List getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return fromJson(rs.getString(columnIndex));
    }

    @Override
    public List getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return fromJson(cs.getString(columnIndex));
    }
}
