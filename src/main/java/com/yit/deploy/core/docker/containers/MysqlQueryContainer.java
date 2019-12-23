package com.yit.deploy.core.docker.containers;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.model.ProcessLauncher;
import com.yit.deploy.core.model.MysqlDBConnection;
import com.yit.deploy.core.function.Lambda;
import hudson.FilePath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by nick on 24/10/2017.
 */
public class MysqlQueryContainer extends DBQueryContainer {
    private String binaryName = "mysql";

    public MysqlQueryContainer(JobExecutionContext executionContext, MysqlDBConnection conn, String ... runOptions) {
        super(executionContext,
            executionContext.getVariable("DOCKER_REGISTRY", String.class) + "/official/mysql:5.6",
            conn,
            runOptions);
    }

    public MysqlQueryContainer mysqldump() {
        MysqlQueryContainer c = (MysqlQueryContainer) clone();
        c.binaryName = "mysqldump";
        return c;
    }

    /**
     * create database if it does not exist
     * @return return true means it really create a database while return false means the database already exists
     */
    public boolean createDatabase() {
        if (isDatabaseExists()) {
            return false;
        }
        rootQuery().query("create database " + conn.getDbName() + " charset = 'utf8'");
        return true;
    }

    public boolean isDatabaseExists() {
        return rootQuery().query("show databases").contains(conn.getDbName());
    }

    public void grantPrivilegesToDBUser() {
        rootQuery().query("GRANT ALL ON " + conn.getDbName() + ".* TO " + conn.getUserName() + "@'%' IDENTIFIED BY '" + conn.getPassword() + "'");
    }

    @Override
    public List<String> getCommandForQuery() {
        return getMysqlCommand("-ss");
    }

    @Override
    public List<String> getCommandForExecute() {
        return getMysqlCommand();
    }

    @Override
    public String getDbName() {
        return isRootQuery() ? "mysql" : conn.getDbName();
    }

    private List<String> getMysqlCommand(String ... args) {
        String passwordArg = "-p" + getPassword();
        if (isEscapePassword()) {
            passwordArg = "'" + passwordArg + "'";
        }

        List<String> cmd = new ArrayList<>(Arrays.asList(
                binaryName, "-h", conn.getHost(), "-P", String.valueOf(conn.getPort()),
                "-u", getUserName(), passwordArg,
                "--default-character-set=utf8"
        ));
        cmd.addAll(Arrays.asList(args));
        if (getQueryOptions() != null) cmd.addAll(getQueryOptions());
        cmd.add(getDbName());
        cmd.addAll(getArguments());
        return cmd;
    }
}
