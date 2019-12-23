package com.yit.deploy.core.support;

import com.yit.deploy.core.dsl.parse.VariableContext;
import com.yit.deploy.core.variables.variable.EncryptedVariable;
import com.yit.deploy.core.variables.variable.SimpleVariable;

public interface EncryptedVariableTypesSupport {
    default VariableContext<String> encrypted(String value) {
        return new VariableContext<>(new EncryptedVariable(new SimpleVariable<>(value)));
    }

    default VariableContext<String> decrypted(String value) {
        return new VariableContext<>(new SimpleVariable<>(value));
    }
}
