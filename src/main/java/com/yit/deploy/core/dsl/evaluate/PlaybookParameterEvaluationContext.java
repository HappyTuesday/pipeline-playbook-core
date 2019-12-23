package com.yit.deploy.core.dsl.evaluate;

import com.yit.deploy.core.dsl.BaseContext;
import com.yit.deploy.core.dsl.support.PlaybookParametersVarSupport;
import com.yit.deploy.core.variables.Variables;

public class PlaybookParameterEvaluationContext extends BaseContext implements PlaybookParametersVarSupport {
    public PlaybookParameterEvaluationContext(Variables vars) {
        resolveVars(vars);
    }
}
