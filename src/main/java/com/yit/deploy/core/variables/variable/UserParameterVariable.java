package com.yit.deploy.core.variables.variable;

import com.yit.deploy.core.dsl.evaluate.JobEvaluationContext;
import com.yit.deploy.core.exceptions.MissingVariableException;
import com.yit.deploy.core.info.UserParameterInfo;
import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.model.VariableType;
import com.yit.deploy.core.model.ProjectParameter;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.variables.resolvers.ResolveContext;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by nick on 31/08/2017.
 */
public class UserParameterVariable<T> extends BaseVariable<T> {
    private String type;
    private String description;
    private boolean required;
    private boolean hidden;
    private boolean persistent;
    private ListVariable<T> choices;
    private Integer order;
    private MapVariable<Object> options;

    private final Variable<T> defaultValue;

    public UserParameterVariable(Variable<T> defaultValue) {
        this(defaultValue, null, null);
    }

    public UserParameterVariable(Variable<T> defaultValue, VariableName name, String id) {
        super(name, id);
        this.defaultValue = defaultValue == null ? null : defaultValue.context(null);
    }

    private void copyFrom(UserParameterVariable<T> p) {
        this.type = p.type;
        this.description = p.description;
        this.required = p.required;
        this.hidden = p.hidden;
        this.persistent = p.persistent;
        this.choices = p.choices;
        this.order = p.order;
        this.options = p.options;
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
    public UserParameterVariable<T> name(@Nonnull VariableName name) {
        if (Objects.equals(this.name, name)) {
            return this;
        }
        UserParameterVariable<T> p = new UserParameterVariable<>(defaultValue, name, id);
        p.copyFrom(this);
        return p;
    }

    /**
     * get the value of this variable, only for override by its implementations, not for outer world
     *
     * @param context resolving context
     * @return the value of the variable
     */
    @Override
    public T get(ResolveContext context) {
        T dv = defaultValue == null ? null : defaultValue.resolve(context);

        String paramName = name.toString();
        if (context.userParameters != null && context.userParameters.containsKey(paramName)) {
            Object value = context.userParameters.get(paramName);
            if (dv == null) {
                //noinspection unchecked
                return (T) value;
            }
            if (dv instanceof Boolean && value instanceof String) {
                //noinspection unchecked
                return (T) (Object) "true".equals(value);
            }
            //noinspection unchecked
            return (T) DefaultGroovyMethods.asType(value, dv.getClass());
        } else if (dv != null) {
            return dv;
        } else if (required) {
            throw new MissingVariableException(name);
        } else {
            return null;
        }
    }

    /**
     * convert variable to variable info
     *
     * @return variable info
     */
    @Override
    public VariableInfo toInfo() {
        VariableInfo vi = new VariableInfo(name, id, VariableType.userParameter, defaultValue);

        UserParameterInfo upi = new UserParameterInfo();
        upi.setType(type);
        upi.setDescription(description);
        upi.setRequired(required);
        upi.setHidden(hidden);
        upi.setPersistent(persistent);
        if (choices != null) {
            upi.setChoices(choices.toInfo());
        }
        upi.setOrder(order);
        if (options != null) {
            upi.setOptions(options.toInfo());
        }

        vi.setOption(upi);
        return vi;
    }

    public ProjectParameter toProjectParameter(JobEvaluationContext context) {
        ProjectParameter p = new ProjectParameter();
        p.setParameterName(name.toString());
        p.setType(type);
        if (defaultValue != null) {
            p.setDefaultValue(context.getVariable(defaultValue));
        }
        p.setDescription(description);
        p.setHidden(hidden);
        p.setPersistent(persistent);
        if (choices != null) {
            p.setChoices((List) context.concreteVariable(choices));
        }
        if (p.getDefaultValue() != null && p.getChoices() != null) {
            // ensure that the default-value is always at the first place of the choices
            p.getChoices().remove(p.getDefaultValue());
            //noinspection unchecked
            p.getChoices().add(0, p.getDefaultValue());
        }
        p.setOrder(order);
        p.setRequired(required);
        if (options != null) {
            //noinspection unchecked
            p.setOptions((Map<String, Object>) context.concreteVariable(options));
        }

        return p;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public ListVariable<T> getChoices() {
        return choices;
    }

    public void setChoices(ListVariable<T> choices) {
        this.choices = choices;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public MapVariable<Object> getOptions() {
        return options;
    }

    public void setOptions(MapVariable<Object> options) {
        this.options = options;
    }
}
