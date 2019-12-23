package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.exceptions.IllegalConfigException;
import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.model.VariableType;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.utils.EncryptionUtils;
import com.yit.deploy.core.variables.resolvers.ResolveContext;

import javax.annotation.Nonnull;
import java.util.Objects;

public class EncryptedVariable extends BaseVariable<String> {

    private final Variable encrypted;

    public EncryptedVariable(Variable encrypted) {
        this(encrypted, null, null);
    }

    public EncryptedVariable(Variable encrypted, VariableName name, String id) {
        super(name, id);
        this.encrypted = encrypted.context(null);
    }

    /**
     * create a new variable instance with a different name
     * <p>
     * NOTE: name field must be readonly, any change to the name field of a variable will create a new instance
     *
     * @param name the new variable name
     * @return new created variable
     */
    @Override
    public EncryptedVariable name(@Nonnull VariableName name) {
        if (Objects.equals(this.name, name)) {
            return this;
        }
        return new EncryptedVariable(encrypted, name, id);
    }

    /**
     * get the value of this variable, only for override by its implementations, not for outer world
     *
     * @param context resolving context
     * @return the value of the variable
     */
    @Override
    public String get(ResolveContext context) {
        if (context.env == null) {
            throw new IllegalConfigException("env is not set in resolve context, could not decrypt variable");
        }
        return new EncryptionUtils(context.env.getEnvtype()).decryptToText((String) encrypted.resolve(context));
    }

    /**
     * convert variable to variable info
     *
     * @return variable info
     */
    @Override
    public VariableInfo toInfo() {
        return new VariableInfo(name, id, VariableType.encrypted, encrypted.toInfo());
    }
}
