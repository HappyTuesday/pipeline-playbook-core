package com.yit.deploy.core.dsl.evaluate;

import com.yit.deploy.core.model.DeployModelTable;
import com.yit.deploy.core.model.Environment;
import com.yit.deploy.core.variables.resolvers.ResolveContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class EnvironmentEvaluationContext extends EvaluationContext {

    private final Environment env;

    public EnvironmentEvaluationContext(Environment env, DeployModelTable modelTable) {
        super(modelTable);
        this.env = env;
        resolveVars(env.getVars());
    }

    public Environment getEnv() {
        return env;
    }

    @Override
    public EnvironmentEvaluationContext getExecutionContext() {
        return this;
    }

    /**
     * get the resolve context
     *
     * @return resolve context
     */
    @Override
    public ResolveContext getResolveContext() {
        return super.getResolveContext().env(env);
    }

    public List<String> groupservers(String hostGroupName) {
        return env.getGroupServers(hostGroupName);
    }

    public Object getEnvVar(String variableName, String envName) {
        return getEnvVar(variableName, modelTable.getEnv(envName));
    }

    public Object getEnvVar(String variableName, Environment env) {
        return new EnvironmentEvaluationContext(env, modelTable).getVariable(variableName);
    }

    public List<Environment> filterEnvs(Predicate<Environment> predicate) {
        List<Environment> list = new ArrayList<>();
        for (Environment env : modelTable.getEnvs()) {
            if (predicate.test(env)) {
                list.add(env);
            }
        }
        return list;
    }
}
