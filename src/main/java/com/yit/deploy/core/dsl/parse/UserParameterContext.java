package com.yit.deploy.core.dsl.parse;

import com.yit.deploy.core.variables.variable.SimpleVariable;
import com.yit.deploy.core.variables.variable.UserParameterVariable;
import com.yit.deploy.core.variables.variable.Variable;

import java.util.*;

/**
 * Created by nick on 31/08/2017.
 */
public class UserParameterContext<T> extends VariableContext<T> {

    public UserParameterContext(T value, String type) {
        this(new SimpleVariable<>(value), type);
    }

    public UserParameterContext(Variable<T> variable, String type) {
        super(new UserParameterVariable<>(variable));
        getUserParameter().setType(type);
    }

    private UserParameterVariable<T> getUserParameter() {
        return (UserParameterVariable<T>) variable;
    }

    public UserParameterContext<T> description(String description) {
        getUserParameter().setDescription(description);
        return this;
    }

    public UserParameterContext<T> required() {
        getUserParameter().setRequired(true);
        return this;
    }

    public UserParameterContext<T> hidden() {
        getUserParameter().setHidden(true);
        return this;
    }

    @SafeVarargs
    public final UserParameterContext<T> choices(T ... choices) {
        return choices(Arrays.asList(choices));
    }

    public final UserParameterContext<T> choices(List<T> choices) {
        getUserParameter().setChoices(Variable.listVariable(choices));
        return this;
    }

    public UserParameterContext<T> persistent() {
        getUserParameter().setPersistent(true);
        return this;
    }

    public UserParameterContext<T> order(int value) {
        getUserParameter().setOrder(value);
        return this;
    }

    public UserParameterContext<T> options(Map<String, Object> options) {
        getUserParameter().setOptions(Variable.mapVariable(options));
        return this;
    }

    public UserParameterContext<T> type(String type) {
        getUserParameter().setType(type);
        return this;
    }
}
