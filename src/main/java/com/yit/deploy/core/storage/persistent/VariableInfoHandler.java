package com.yit.deploy.core.storage.persistent;

import com.yit.deploy.core.info.VariableInfo;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VariableInfoHandler extends BaseTypeHandler<VariableInfo> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, VariableInfo parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.toJson());
    }

    @Override
    public VariableInfo getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return VariableInfo.fromJson(rs.getString(columnName));
    }

    @Override
    public VariableInfo getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return VariableInfo.fromJson(rs.getString(columnIndex));
    }

    @Override
    public VariableInfo getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return VariableInfo.fromJson(cs.getString(columnIndex));
    }
}
