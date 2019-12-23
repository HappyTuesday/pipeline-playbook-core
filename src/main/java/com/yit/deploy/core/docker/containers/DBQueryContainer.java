package com.yit.deploy.core.docker.containers;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.DBConnection;
import com.yit.deploy.core.model.MysqlDBConnection;
import com.yit.deploy.core.model.ProcessLauncher;
import hudson.FilePath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by nick on 24/10/2017.
 */
public abstract class DBQueryContainer extends DockerContainer {
    protected DBConnection conn;

    private boolean rootQuery;
    private List<String> queryOptions;
    private List<String> arguments = Collections.emptyList();
    private boolean escapePassword = false;

    public DBQueryContainer(JobExecutionContext executionContext,
                            String imageFullName,
                            DBConnection conn,
                            String ... runOptions) {

        super(executionContext, imageFullName, runOptions);
        this.conn = conn;
    }

    public DBQueryContainer rootQuery() {
        DBQueryContainer c = (DBQueryContainer) clone();
        c.rootQuery = true;
        return c;
    }

    public DBQueryContainer queryOptions(String... queryOptions) {
        DBQueryContainer c = (DBQueryContainer) clone();
        if (c.queryOptions == null) {
            c.queryOptions = Arrays.asList(queryOptions);
        } else {
            c.queryOptions = new ArrayList<>(c.queryOptions);
            c.queryOptions.addAll(Arrays.asList(queryOptions));
        }
        return c;
    }

    public DBQueryContainer arguments(List<String> arguments) {
        DBQueryContainer c = (DBQueryContainer) clone();
        c.arguments = arguments;
        return c;
    }

    public DBQueryContainer conn(MysqlDBConnection conn) {
        DBQueryContainer c = (DBQueryContainer) clone();
        c.conn = conn;
        return c;
    }

    public String querySingle(String sql) {
        List<String> lines = query(sql);
        return lines.size() > 0 ? lines.get(0) : null;
    }

    public List<String> query(String sql) {
        return queryInternal(sql);
    }

    private List<String> queryInternal(String sql) {
        getScript().debug("execute sql [ %s ] against %s", sql, getDbName());
        return createProcessForQuery().input(sql).executeReturnLines();
    }

    public void executeScript(String sql) {
        getScript().debug("execute sql [ %s ] against %s", sql, getDbName());
        createProcessForExecute().input(sql).executeIgnoreOutput();
    }

    public void executeScriptFile(FilePath file) {
        getScript().debug("execute sql file %s against %s", file.getRemote(), getDbName());
        createProcessForExecute().input(file).executePrintOutput();
    }

    public ProcessLauncher createProcessForQuery() {
        return createProcessLauncher(getCommandForQuery());
    }

    public ProcessLauncher createProcessForExecute() {
        return createProcessLauncher(getCommandForExecute());
    }

    public String getExecuteScriptCommandLine() {
        DBQueryContainer c = (DBQueryContainer) clone();
        c.escapePassword = true;
        return Lambda.join(" ", c.getDockerExecCommand(c.getCommandForExecute()));
    }

    public abstract List<String> getCommandForQuery();

    public abstract List<String> getCommandForExecute();

    public abstract String getDbName();

    public String getUserName() {
        return rootQuery ? conn.getRootUserName() : conn.getUserName();
    }

    public String getPassword() {
        return rootQuery ? conn.getRootPassword() : conn.getPassword();
    }

    public DBConnection getConn() {
        return conn;
    }

    public boolean isEscapePassword() {
        return escapePassword;
    }

    public boolean isRootQuery() {
        return rootQuery;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public List<String> getQueryOptions() {
        return queryOptions;
    }
}
