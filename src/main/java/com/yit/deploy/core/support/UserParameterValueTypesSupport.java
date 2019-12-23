package com.yit.deploy.core.support;

import com.yit.deploy.core.dsl.parse.UserParameterContext;
import com.yit.deploy.core.dsl.execute.TaskExecutionContext;
import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.parameters.inventory.DeployInventory;
import com.yit.deploy.core.variables.variable.Variable;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

/**
 * Created by nick on 01/09/2017.
 */
public interface UserParameterValueTypesSupport {

    default <T> UserParameterContext<T> parameter(T value) {
        return new UserParameterContext<>(Variable.toVariable(value), "string");
    }

    default <T> UserParameterContext<T> parameter(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = TaskExecutionContext.class) Closure<T> closure) {
        return new UserParameterContext<>(Variable.lazyVariable(new ClosureWrapper<>(closure)), "string");
    }

    default UserParameterContext<Boolean> parameter(boolean value) {
        return new UserParameterContext<>(Variable.toVariable(value), "boolean");
    }

    default UserParameterContext<String> projectBranchParameter(Object value) {
        return new UserParameterContext<>(Variable.toVariableCast(value), "projectBranch");
    }

    default UserParameterContext<String> testCaseParameter(Object value) {
        return new UserParameterContext<>(Variable.toVariableCast(value), "testCase");
    }

    default UserParameterContext<DeployInventory> deployInventoryParameter(DeployInventory inventory) {
        return new UserParameterContext<>(Variable.toVariable(inventory), "deployInventory");
    }
}
