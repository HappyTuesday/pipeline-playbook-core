package com.yit.deploy.core.support;

import com.yit.deploy.core.dsl.evaluate.EvaluationContext;
import com.yit.deploy.core.model.JinjaTemplate;
import com.yit.deploy.core.variables.resolvers.VariableResolver;
import com.yit.deploy.core.variables.variable.EncryptedVariable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface JinjaSupport {
    EvaluationContext getExecutionContext();

    /**
     * a jinja template with full permissions to variables (or permissions directed inherited from parent)
     * @return jinja template
     */
    default JinjaTemplate createJinjaTemplate() {
        return new JinjaTemplate(getExecutionContext());
    }

    /**
     * a jinja template who can only access non-encrypted variables
     * @return jinja template
     */
    default JinjaTemplate createRestrictedJinjaTemplate() {
        return new JinjaTemplate(getExecutionContext().withAuthorization(v -> !(v instanceof EncryptedVariable)));
    }

    /**
     * a jinja template who can only access variables in a whitelist)
     * @return jinja template
     */
    default JinjaTemplate createRestrictedJinjaTemplate(String variableName, String ... variableWhitelist) {
        Set<String> set = new HashSet<>(variableWhitelist.length + 1);
        set.add(variableName);
        set.addAll(Arrays.asList(variableWhitelist));
        return new JinjaTemplate(getExecutionContext().withAuthorization(v -> set.contains(v.name().toString())));
    }
}
