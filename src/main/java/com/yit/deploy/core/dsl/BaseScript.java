package com.yit.deploy.core.dsl;

import com.yit.deploy.core.support.CodeRegionSupport;
import com.yit.deploy.core.variables.resolvers.SimpleVariableResolver;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.variables.resolvers.VariableResolverProxy;
import groovy.lang.Script;

public abstract class BaseScript extends Script implements VariableResolverProxy, CodeRegionSupport {

    private VariableResolver variableResolver = new SimpleVariableResolver();

    public VariableResolver getVariableResolver() {
        return variableResolver;
    }

    @Override
    public Object getProperty(String property) {
        return getMetaClass().getProperty(this, property);
    }

    @Override
    public void setProperty(String property, Object newValue) {
        getMetaClass().setProperty(this, property, newValue);
    }
}
