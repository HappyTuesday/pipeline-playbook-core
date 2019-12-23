package com.yit.deploy.core.dsl.evaluate;

import com.yit.deploy.core.dsl.BaseContext;
import com.yit.deploy.core.model.DeployModelTable;
import com.yit.deploy.core.support.*;
import com.yit.deploy.core.variables.VariableAuthorizer;
import com.yit.deploy.core.variables.resolvers.ResolveContext;

public class EvaluationContext
    extends BaseContext
    implements QueriesSupport, ExceptionSupport, AlgorithmSupport, ProcessSupport, JinjaSupport,
    VariableAuthorizationSupport, Cloneable {

    // it should be final, but for the sake of convenience to clone, we remove the final modifier
    private VariableAuthorizer authorizer;
    protected final DeployModelTable modelTable;

    public EvaluationContext(DeployModelTable modelTable) {
        this.modelTable = modelTable;
    }

    public DeployModelTable getModelTable() {
        return modelTable;
    }

    @Override
    public EvaluationContext getExecutionContext() {
        return this;
    }

    /**
     * get the resolve context
     *
     * @return resolve context
     */
    @Override
    public ResolveContext getResolveContext() {
        return super.getResolveContext().authorizing(authorizer);
    }

    public EvaluationContext withAuthorization(VariableAuthorizer newAuthorizer) {
        EvaluationContext clone;
        try {
            clone = (EvaluationContext) clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }

        VariableAuthorizer oldAuthorizer = this.authorizer;
        if (oldAuthorizer == null) {
            clone.authorizer = newAuthorizer;
        } else {
            clone.authorizer = v -> oldAuthorizer.authorize(v) && newAuthorizer.authorize(v);
        }
        return clone;
    }
}
