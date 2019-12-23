package com.yit.deploy.core.dsl.parse;

import com.yit.deploy.core.config.DeployConfig;
import com.yit.deploy.core.dsl.BaseScript;
import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.info.EnvironmentInfo;
import com.yit.deploy.core.info.HostInfo;
import com.yit.deploy.core.model.Environment;
import com.yit.deploy.core.model.Host;
import com.yit.deploy.core.support.EncryptedVariableTypesSupport;
import com.yit.deploy.core.variables.LayeredVariables;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.function.Action;
import com.yit.deploy.core.model.MysqlDBConnection;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.support.ClosuresSupport;
import com.yit.deploy.core.support.VariableValueTypesSupport;
import com.yit.deploy.core.utils.Utils;
import com.yit.deploy.core.variables.variable.EncryptedVariable;
import com.yit.deploy.core.variables.variable.SimpleVariable;
import com.yit.deploy.core.variables.variable.Variable;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Supplier;

public abstract class EnvironmentBaseScript extends BaseScript implements VariableValueTypesSupport, EncryptedVariableTypesSupport {

    private DeployConfig deployConfig;

    private Map<String, EnvironmentInfo> envs;

    private EnvironmentInfo env;

    private LayeredVariables parentVars;

    private Map<String, Object> labelsAssignToHosts = new LinkedHashMap<>();

    public void parse(String name, DeployConfig deployConfig, Map<String, EnvironmentInfo> envs) {
        if (envs.containsKey(name)) {
            throw new IllegalArgumentException("environment " + name + " has already been parsed");
        }
        this.deployConfig = deployConfig;
        this.envs = envs;
        this.env = new EnvironmentInfo(name);

        this.parentVars = new LayeredVariables();
        getVariableResolver().resolveVars(parentVars, env.getVars());
        getVariableResolver().setWritableVars(env.getVars());

        envs.put(name, this.env);

        refreshParentVars();
        run();
    }

    private void refreshParentVars() {
        parentVars.clearLayers();
        for (String e : env.descendingParents(envs)) {
            parentVars.layer(envs.get(e).getVars());
        }
    }

    public EnvironmentInfo getEnv() {
        return env;
    }

    public void desc(String description) {
        env.setDescription(description);
    }

    public HostGroupContext group(String name) {
        return group(name, null);
    }

    public HostGroupContext group(String name, @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = HostGroupContext.class) Closure closure) {
        HostGroupContext context = new HostGroupContext(env, envs, name);
        if (closure != null) {
            Closures.delegateOnly(context, closure);
        }
        return context;
    }

    public HostContext host(String hostname) {
        return host(hostname, null);
    }

    public HostContext host(String hostname, @DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = HostContext.class) Closure closure) {
        HostInfo host = env.getHosts().computeIfAbsent(hostname, HostInfo::new);
        host.getLabels().putAll(this.labelsAssignToHosts);
        HostContext c = new HostContext(host, env, envs);
        if (closure != null) {
            Closures.delegateOnly(c, closure);
        }
        return c;
    }

    public void labelHosts(String name, Object value, Runnable body) {
        this.labelsAssignToHosts.put(name, value);
        body.run();
        this.labelsAssignToHosts.remove(name);
    }

    public void abstracted() {
        abstracted(true);
    }

    public void abstracted(boolean abstracted) {
        this.env.setAbstracted(abstracted);
    }

    public void inherits(String envName) {
        if (!envs.containsKey(envName)) {
            deployConfig.getEnvironmentScript(envName).parse(envName, deployConfig, envs);
        }
        EnvironmentInfo inherited = envs.get(envName);
        if (inherited.isAbstracted() == null || !inherited.isAbstracted()) {
            throw new IllegalConfigException("environment " + envName + " is not abstracted but is inherited by " + env.getName());
        }
        if (env.getParents().contains(envName)) {
            throw new IllegalConfigException("environment " + envName + " is already inherited by " + env.getName());
        }
        env.getParents().add(envName);

        refreshParentVars();
    }

    public void defaultDeployUser(Object value) {
        setVariable(Environment.DEFAULT_DEPLOY_USER_VARIABLE, Variable.toVariable(value));
    }

    public void labels(String ... labels) {
        Lambda.uniqueAdd(env.getLabels(), labels);
    }

    public void declaredAsProdEnv() {
        this.labels(Environment.PROD_ENV_LABEL);
    }

    public void declaredAsTestEnv() {
        this.labels(Environment.TEST_ENV_LABEL);
    }

    public void declaredAsLocalEnv() {
        this.labels(Environment.LOCAL_ENV_LABEL);
    }

    public void defineMysqlDBVariables(
            Collection<String> keys,
            @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = MysqlDBVariableDsl.class)
            @ClosureParams(value = SimpleType.class, options = {"java.lang.String"})
                    Closure closure) {

        for (String key : keys) {
            defineMysqlDBVariables(key, closure);
        }
    }

    public MysqlDBVariableDsl defineMysqlDBVariables(String key) {
        return defineMysqlDBVariables(key, null);
    }

    public MysqlDBVariableDsl defineMysqlDBVariables(
            String key,
            @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = MysqlDBVariableDsl.class)
            @ClosureParams(value = SimpleType.class, options = {"java.lang.String"})
                    Closure<?> closure) {

        MysqlDBVariableDsl dsl = new MysqlDBVariableDsl(getVariableResolver(), key);
        if (closure != null) {
            Closures.with(dsl, closure, key);
        }
        return dsl;
    }

    public static class MysqlDBVariableDsl implements ClosuresSupport, VariableValueTypesSupport {

        private VariableResolver resolver;
        private String key;
        private String dbShortName;
        private Map<String, Object> connectionOptions = new HashMap<>();
        private boolean urlAutoEscape = false;
        private String currentCatalog;
        private Supplier<Object> currentDbNameGenerator;
        private String characterSetName = "utf8";

        MysqlDBVariableDsl(VariableResolver resolver, String key) {
            connectionOptions.put("autoReconnect", true);
            connectionOptions.put("useUnicode", true);
            connectionOptions.put("characterset", "utf-8");
            connectionOptions.put("allowMultiQueries", true);

            currentDbNameGenerator = () -> {
                String copyFromName = getCopyFromVariableNamePrefix(), dbShortName = this.dbShortName;
                if (copyFromName != null) {
                    return variableClosure(dsl -> dsl.getVariable(copyFromName + "_NAME"));
                } else {
                    return variableClosure(dsl -> String.format("yit_%s_%s", dsl.getVariable("ENV"), dbShortName));
                }
            };
            this.resolver = resolver;
            this.key = key.toUpperCase();
            this.dbShortName = key.toLowerCase();
        }

        private String getConnectionOptionsString() {
            return String.join(urlAutoEscape ? "&amp;" : "&", Lambda.map(connectionOptions, (key, value) -> {
                try {
                    return key + "=" + URLEncoder.encode(String.valueOf(value), Utils.DefaultCharset.name());
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        private String getVariableNamePrefix() {
            return (currentCatalog != null ? key + "_" + currentCatalog : key) + "_DB";
        }

        private String getCopyFromVariableNamePrefix() {
            return currentCatalog != null ? key + "_DB" : null;
        }

        public MysqlDBVariableDsl catalog(String catalog) {
            this.currentCatalog = catalog;
            return this;
        }

        public void catalog(String catalog, Action action) {
            String origin = this.currentCatalog;
            this.currentCatalog = catalog;
            try {
                action.run();
            } finally {
                this.currentCatalog = origin;
            }
        }

        public void dbNameGenerator(Supplier<Object> dbNameGenerator, Action action) {
            Supplier<Object> origin = this.currentDbNameGenerator;
            this.currentDbNameGenerator = dbNameGenerator;
            try {
                action.run();
            } finally {
                this.currentDbNameGenerator = origin;
            }
        }

        public void dbShortName(String name) {
            this.dbShortName = name;
        }

        /**
         * create a set of variables for a special catalog of MYSQL DB related configurations
         */
        public MysqlDBVariableDsl withDefaults() {
            String key = this.key;
            String connOpt = this.getConnectionOptionsString();
            String name = getVariableNamePrefix();
            String copyFromName = getCopyFromVariableNamePrefix();

            // MAGENTO_READONLY_HOST = { MAGENTO_HOST }
            resolver.setVariable(
                    name + "_HOST",
                    copyFromName != null ? variableClosure(dsl -> dsl.getVariable(copyFromName + "_HOST")) : variableClosure(dsl -> dsl.getVariable("MYSQL_HOST")));

            resolver.setVariable(name + "_NAME", currentDbNameGenerator.get());

            resolver.setVariable(
                    name + "_PORT",
                    copyFromName != null ? variableClosure(dsl -> dsl.getVariable(copyFromName + "_PORT")) : variableClosure(dsl -> dsl.getVariable("MYSQL_PORT")));

            resolver.setVariable(
                    name + "_USERNAME",
                    copyFromName != null ? variableClosure(dsl -> dsl.getVariable(copyFromName + "_USERNAME")) : abstractedVariable(String.class));

            resolver.setVariable(
                    name + "_PASSWORD",
                    copyFromName != null ? variableClosure(dsl -> dsl.getVariable(copyFromName + "_PASSWORD")) : abstractedVariable(String.class));

            resolver.setVariable(
                    name + "_ROOT_USERNAME",
                    variableClosure(dsl -> dsl.getVariable(name + "_USERNAME")));

            resolver.setVariable(
                    name + "_ROOT_PASSWORD",
                    variableClosure(dsl -> dsl.getVariable(name + "_PASSWORD")));

            resolver.setVariable(
                    name + "_URL",
                    variableClosure(dsl ->
                        String.format("jdbc:mysql://%s:%d/%s?%s", dsl.getVariable(name + "_HOST"), dsl.getVariable(name + "_PORT", Integer.class), dsl.getVariable(name + "_NAME"), connOpt)
                    ));

            resolver.setVariable(
                    name,
                    variableClosure(dsl -> {
                        MysqlDBConnection conn = new MysqlDBConnection();
                        conn.setHost(dsl.getVariable(name + "_HOST", String.class));
                        conn.setPort(dsl.getVariable(name + "_PORT", Integer.class));
                        conn.setDbName(dsl.getVariable(name + "_NAME", String.class));
                        conn.setUserName(dsl.getVariable(name + "_USERNAME", String.class));
                        conn.setPassword(dsl.getVariable(name + "_PASSWORD", String.class));
                        conn.setRootUserName(dsl.getVariable(name + "_ROOT_USERNAME", String.class));
                        conn.setRootPassword(dsl.getVariable(name + "_ROOT_PASSWORD", String.class));
                        conn.setCharacterSetName(characterSetName);
                        conn.setUrl(dsl.getVariable(name + "_URL", String.class));
                        return conn;
                    }));

            return this;
        }

        public MysqlDBVariableDsl withReadonlyDefaults() {
            readonly(this::withDefaults);
            return this;
        }

        public MysqlDBVariableDsl withAutotestDefaults() {
            autotest(this::withDefaults);
            return this;
        }

        public MysqlDBVariableDsl withBackupDefaults() {
            backup(this::withDefaults);
            return this;
        }

        public MysqlDBVariableDsl readonly() {
            return catalog("READONLY");
        }

        public void readonly(Action action) {
            catalog("READONLY", action);
        }

        public MysqlDBVariableDsl autotest() {
            return catalog("AUTOTEST");
        }

        public void autotest(Action action) {
            catalog("AUTOTEST", () -> dbNameGenerator(() -> {
                String dbShortName = this.dbShortName;
                return variableClosure(dsl -> String.format("yit_%s_autotest_%s", dsl.getVariable("ENV"), dbShortName));
            }, action));
        }

        public void backup(Action action) {
            catalog("BACKUP", () -> dbNameGenerator(() -> {
                String dbShortName = this.dbShortName;
                return variableClosure(dsl -> String.format("yit_backup_%s", dbShortName));
            }, action));
        }

        public MysqlDBVariableDsl connectionOption(Map<String, Object> opts) {
            this.connectionOptions.putAll(opts);
            return this;
        }

        public MysqlDBVariableDsl connectionOptions(Map<String, Object> opts) {
            this.connectionOptions = opts;
            return this;
        }

        public MysqlDBVariableDsl urlAutoEscape() {
            return urlAutoEscape(true);
        }

        public MysqlDBVariableDsl urlAutoEscape(boolean value) {
            this.urlAutoEscape = value;
            return this;
        }

        public MysqlDBVariableDsl override(String type, Object value) {
            resolver.setVariable(type != null ? getVariableNamePrefix() + "_" + type : getVariableNamePrefix(), value);
            return this;
        }

        public MysqlDBVariableDsl host(Object value) {
            return override("HOST", value);
        }

        public MysqlDBVariableDsl port(Object value) {
            return override("PORT", value);
        }

        public MysqlDBVariableDsl dbName(Object value) {
            return override("NAME", value);
        }

        public MysqlDBVariableDsl username(Object value) {
            return override("USERNAME", value);
        }

        public MysqlDBVariableDsl password(Object value) {
            return override("PASSWORD", value);
        }

        public MysqlDBVariableDsl rootUserName(Object value) {
            return override("ROOT_USERNAME", value);
        }

        public MysqlDBVariableDsl rootPassword(Object value) {
            return override("ROOT_PASSWORD", value);
        }

        public MysqlDBVariableDsl url(Object value) {
            return override("URL", value);
        }

        public MysqlDBVariableDsl db(Object value) {
            return override(null, value);
        }

        public MysqlDBVariableDsl characterSetName(String value) {
            this.characterSetName = value;
            return this;
        }

        public MysqlDBVariableDsl utf8mb4(){
            return characterSetName("utf8mb4");
        }
    }
}