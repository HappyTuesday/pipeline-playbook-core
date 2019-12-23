package com.yit.deploy.core.steps;

import com.yit.deploy.core.dsl.execute.JobExecutionContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.model.JinjaTemplate;
import com.yit.deploy.core.support.ContainersSupport;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.model.MysqlDBConnection;
import com.yit.deploy.core.function.Closures;
import com.yit.deploy.core.utils.IO;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

import java.util.*;

/**
 * Created by nick on 17/10/2017.
 */
public class DisconfStep extends AbstractStep implements ContainersSupport {

    public static final String PREVIOUS_VALUE_FILETER_NAME = "previousValue";

    private String apiHost;
    private int apiPort = 80;
    private String apiAdminPassword;
    private MysqlDBConnection dbConn;
    private String state = "present";
    private boolean autoLogin = true;

    private String appName;
    private Integer _appId;
    private String envName;
    private Integer _envId;
    private String configKey;
    private String version = "0.1.0.0";
    private String configType = "file";
    private String configValue;
    private String template;

    private UriStep uriStep;
    private List<String> changes = new ArrayList<>();
    private JinjaTemplate jinja;
    private String disconfDomain;

    private static ThreadLocal<Properties> currentProperties = new ThreadLocal<>();

    public DisconfStep(JobExecutionContext context) {
        super(context);
        jinja = new JinjaTemplate(context);
        disconfDomain = (String) context.getVariable("DISCONF_DOMAIN");
    }

    public DisconfStep setup(@DelegatesTo(value = DslContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
        Closures.with(new DslContext(this), closure);
        return this;
    }

    public List<String> getChanges() {
        return changes;
    }

    /**
     * Execute this step, providing a pipeline-script and a variable context
     * add / update or delete a disconf item/file config. ensure you have logged in before calling this method and will loggout after calling if autoLogin is false
     */
    @Override
    protected Object executeOverride() {
        assert configValue != null || template != null;

        if (autoLogin) {
            login();
            try {
                manageConfig();
            } finally {
                currentProperties.remove();
                logout();
            }
        } else {
            manageConfig();
        }
        return null;
    }

    private void executeSql(String sql) {
        withMysqlQuery(dbConn, c -> c.executeScript(sql));
    }

    private Map callApi(String methodName, String api, Map<String, Object> paramsValue, Object payloadValue) {
        return callApi(methodName, api, paramsValue, payloadValue, false);
    }

    private Map callApi(String methodName, String api, Map<String, Object> paramsValue, Object payloadValue, boolean saveCookieValue) {
        if (uriStep == null) {
            uriStep = new UriStep(context);
            new UriStep.DslContext(uriStep).contentType("application/x-www-form-urlencoded; charset=UTF-8").followRedirects().autoParseJson();
        }

        new UriStep.DslContext(uriStep)
                .method(methodName)
                .url(String.format("http://%s:%s/api/%s", apiHost, apiPort, api))
                .params(paramsValue)
                .payload(payloadValue)
                .saveCookie(saveCookieValue);

        Map res = uriStep.execute(Map.class);

        if (!"true".equals(res.get("success"))) {
            throw new RuntimeException("call disconf api failed: " + res.get("message"));
        }

        return res;
    }

    private Map httpGet(String api) {
        return httpGet(api, null);
    }

    private Map httpGet(String api, Map<String, Object> params) {
        return callApi("GET", api, params, null);
    }

    private Map httpDelete(String api) {
        return httpDelete(api, null);
    }

    private Map httpDelete(String api, Map<String, Object> params) {
        return callApi("DELETE", api, params, null);
    }

    private Map httpPost(String api) {
        return httpPost(api, null);
    }

    private Map httpPost(String api, Object payload) {
        return callApi("POST", api, null, payload);
    }

    private Map httpPut(String api) {
        return httpPut(api, null);
    }

    private Map httpPut(String api, Object payload) {
        return callApi("PUT", api, null, payload);
    }

    public void login() {

        // open a new session to obtain disconf cookie
        // otherwise disconf will NOT save session into redis
        // which will cause login failed when there are multiple disconf instances
        try {
            callApi("GET", "account/session", null, null, true);
        } catch (RuntimeException re) {

        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "admin");
        payload.put("password", apiAdminPassword);
        payload.put("remember", 1);

        callApi("POST", "account/signin", null, payload, true);
    }


    public void logout() {
        httpGet("account/signout");
    }

    @SuppressWarnings("unchecked")
    private List<Map> listApps() {
        return Lambda.subscript(httpGet("app/list"), List.class, "page", "result");
    }

    private Integer getAppId() {
        if (_appId == null) {
            _appId = Lambda.subscript(Lambda.find(listApps(), a -> appName.equals(a.get("name"))), Integer.class, "id");
        }
        return _appId;
    }

    private void createApp() {
        if (getAppId() == null) {
            httpPost("app", Lambda.asMap("app", appName, "desc", appName));
            changes.add("create app " + appName);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map> listEnvs() {
        return Lambda.subscript(httpGet("env/list"), List.class, "page", "result");
    }

    private Integer getEnvId() {
        if (_envId == null) {
            _envId = Lambda.subscript(Lambda.find(listEnvs(), e -> envName.equals(e.get("name"))), Integer.class, "id");
        }
        return _envId;
    }

    private void createEnv() {
        if (getEnvId() == null) {
            executeSql("INSERT INTO env(name) VALUES('" + envName + "')");
            changes.add("create env " + envName);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map> listConfigs() {
        return Lambda.subscript(
                httpGet("web/config/simple/list", Lambda.asMap("appId", getAppId(), "envId", getEnvId(), "version", version)),
                List.class,
                "page", "result");
    }

    private Integer getConfigId() {
        return Lambda.subscript(Lambda.find(listConfigs(), c -> configKey.equals(c.get("key"))), Integer.class, "configId");
    }

    private String getConfigValue(int configId) {
        return Lambda.subscript(httpGet("web/config/" + configId, Lambda.asMap("configId", configId)), String.class, "result", "value");
    }

    private void createConf() {
        createApp();
        createEnv();

        boolean configValueChanged = true;
        Integer configId = getConfigId();
        String oldValue = configId == null ? null : getConfigValue(configId);
        String configValue = this.configValue;

        if (configValue == null) { // means we use template to generate configValue
            configureJinjaEngine(oldValue);
            configValue = jinja.render(template, configKey);
        }

        if (oldValue != null) {
            configValueChanged = !oldValue.trim().equals(configValue.trim());
        }

        if ("item".equals(configType)) {
            if (configId == null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("key", configKey);
                payload.put("value", configValue);
                payload.put("appId", getAppId());
                payload.put("envId", getEnvId());
                payload.put("version", version);

                httpPost("web/config/item", payload);
                changes.add("add config " + constructConfigChange(getConfigId()));
            } else if (configValueChanged) {
                httpPut("web/config/item/" + configId, Lambda.asMap("configId", configId, "value", configValue));
                changes.add("update config " + constructConfigChange(getConfigId()));
            }
        } else {
            if (configId == null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("fileName", configKey);
                payload.put("appId", getAppId());
                payload.put("envId", getEnvId());
                payload.put("version", version);
                payload.put("fileContent", configValue);

                httpPost("web/config/filetext", payload);
                changes.add("add config " + constructConfigChange(getConfigId()));
            } else if (configValueChanged) {
                httpPut("web/config/filetext/" + configId, Lambda.asMap("fileContent", configValue));
                changes.add("update config " + constructConfigChange(getConfigId()));
            }
        }
    }

    private String constructConfigChange(Integer configId) {
        return String.format("http://%s/#/history/%s", disconfDomain, configId);
    }

    private void configureJinjaEngine(String oldValue) {
        final Properties properties;
        if (oldValue != null && configKey.endsWith(".properties")) {
            properties = IO.loadProperties(oldValue);
        } else {
            properties = new Properties();
        }
        currentProperties.set(properties);

        jinja.registerFilter(PREVIOUS_VALUE_FILETER_NAME, (key, args) -> properties.getProperty(String.valueOf(key), args.length > 0 ? args[0] : ""));
        jinja.registerFunction(PREVIOUS_VALUE_FILETER_NAME, DisconfStep.class,"previousValue", String.class, String.class);
    }

    private static String previousValue(String key, String defaultValue) {
        Properties properties = currentProperties.get();
        if (properties == null) {
            throw new IllegalStateException("could not load properties object from thread local");
        }
        return properties.getProperty(key, defaultValue);
    }

    private void deleteConf() {
        if (getAppId() != null && getEnvId() != null) {
            Integer configId = getConfigId();
            if (configId != null) {
                httpDelete("web/config/" + configId);
                changes.add(String.format("delete config item %s with version %s in app %s / env %s", configKey, version, appName, envName));
            }
        }
    }

    private void manageConfig() {
        if ("present".equals(state)) {
            createConf();
        } else if ("absent".equals(state)) {
            deleteConf();
        } else {
            throw new IllegalArgumentException(state);
        }
    }

    public static class DslContext {

        private DisconfStep step;

        public DslContext(DisconfStep step) {
            this.step = step;
        }

        public DslContext apiHost(String value) {
            step.apiHost = value;
            return this;
        }

        public DslContext apiPort(int value) {
            step.apiPort = value;
            return this;
        }

        public DslContext apiAdminPassword(String value) {
            step.apiAdminPassword = value;
            return this;
        }

        public DslContext dbConn(MysqlDBConnection value) {
            step.dbConn = value;
            return this;
        }

        public DslContext present() {
            step.state = "present";
            return this;
        }

        public DslContext absent() {
            step.state = "absent";
            return this;
        }

        public DslContext appName(String value) {
            step.appName = value;
            step._appId = null;
            return this;
        }

        public DslContext envName(String value) {
            step.envName = value;
            step._envId = null;
            return this;
        }

        public DslContext configKey(String value) {
            step.configKey = value;
            return this;
        }

        public DslContext version(String value) {
            step.version = value;
            return this;
        }

        public DslContext useItemConfigType() {
            step.configType = "item";
            return this;
        }

        public DslContext useFileConfigType() {
            step.configType = "file";
            return this;
        }

        public DslContext fromTemplate(String template) {
            step.template = template;
            return this;
        }

        public DslContext configValue(String value) {
            step.configValue = value;
            return this;
        }

        public DslContext autoLogin(boolean value) {
            step.autoLogin = value;
            return this;
        }
    }
}
