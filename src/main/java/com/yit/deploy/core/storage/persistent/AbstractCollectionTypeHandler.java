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
import java.util.ArrayList;
import java.util.List;

public class AbstractCollectionTypeHandler extends BaseTypeHandler<AbstractCollection> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, AbstractCollection parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, DeployRecordTable.GSON.toJson(parameter));
    }

    private static AbstractCollection fromJson(String s) {
        return s == null ? null : DeployRecordTable.GSON.fromJson(s, new TypeToken<AbstractCollection>(){}.getType());
    }

    @Override
    public AbstractCollection getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return fromJson(rs.getString(columnName));
    }

    @Override
    public AbstractCollection getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return fromJson(rs.getString(columnIndex));
    }

    @Override
    public AbstractCollection getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return fromJson(cs.getString(columnIndex));
    }
}
