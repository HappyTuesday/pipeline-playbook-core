package com.yit.deploy.core.support;

import com.yit.deploy.core.variables.resolvers.VariableResolver;

/**
 * Created by nick on 31/10/2017.
 */
public interface QueriesSupport {

    VariableResolver getExecutionContext();
}
