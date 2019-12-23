package com.yit.deploy.core.docker.containers;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.DBConnection;
import com.yit.deploy.core.model.MysqlDBConnection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by nick on 24/10/2017.
 */
public class PostgresQueryContainer extends DBQueryContainer {

    private String binaryName = "psql";

    public PostgresQueryContainer(JobExecutionContext executionContext, DBConnection conn, String ... runOptions) {
        super(executionContext,
            "postgres:9",
            conn,
            Lambda.concat("-e 'PGPASSWORD=" + conn.getPassword() + "'", runOptions).toArray(new String[0]));
    }

    /**
     * create database if it does not exist
     * @return return true means it really create a database while return false means the database already exists
     */
    public boolean createDatabase() {
        if (isDatabaseExists()) {
            return false;
        }
        rootQuery().query("create database " + conn.getDbName());
        return true;
    }

    public boolean isDatabaseExists() {
        return rootQuery().query("SELECT datname FROM pg_database").contains(conn.getDbName());
    }

    @Override
    public List<String> getCommandForQuery() {
        return getPostgresCommand("-AtF,");
    }

    @Override
    public List<String> getCommandForExecute() {
        return getPostgresCommand();
    }

    @Override
    public String getDbName() {
        return isRootQuery() ? "postgres" : conn.getDbName();
    }

    private List<String> getPostgresCommand(String ... args) {
        List<String> cmd = new ArrayList<>(Arrays.asList(
            binaryName,
            "-h", conn.getHost(),
            "-p", String.valueOf(conn.getPort()),
            "-d", getDbName(),
            "-U", getUserName()
        ));
        cmd.addAll(Arrays.asList(args));
        if (getQueryOptions() != null) cmd.addAll(getQueryOptions());
        cmd.addAll(getArguments());
        return cmd;
    }
}
