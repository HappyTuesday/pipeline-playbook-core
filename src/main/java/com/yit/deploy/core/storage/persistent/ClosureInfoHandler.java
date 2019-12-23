package com.yit.deploy.core.storage.persistent;

import com.yit.deploy.core.info.ClosureInfo;
import com.yit.deploy.core.info.VariableInfo;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClosureInfoHandler extends BaseTypeHandler<ClosureInfo> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ClosureInfo parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.toJson());
    }

    @Override
    public ClosureInfo getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return ClosureInfo.fromJson(rs.getString(columnName));
    }

    @Override
    public ClosureInfo getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return ClosureInfo.fromJson(rs.getString(columnIndex));
    }

    @Override
    public ClosureInfo getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return ClosureInfo.fromJson(cs.getString(columnIndex));
    }
}
