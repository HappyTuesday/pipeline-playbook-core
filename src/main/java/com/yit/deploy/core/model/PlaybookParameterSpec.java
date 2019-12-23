package com.yit.deploy.core.model;

import com.yit.deploy.core.function.Lambda;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PlaybookParameterSpec {
    public final String name;
    /**
     * if not null, the target value must be one of this list
     */
    private final List<Object> allowedValues;

    public PlaybookParameterSpec(String name, List<Object> allowedValues) {
        this.name = name;
        this.allowedValues = allowedValues;
    }

    public PlaybookParameterSpec intersect(PlaybookParameterSpec that) {
        if (!Objects.equals(this.name, that.name)) {
            throw new IllegalArgumentException("only playbook parameter specs with the same name can be intersected");
        }
        List<Object> alv;
        if (this.allowedValues == null) {
            alv = that.allowedValues;
        } else if (that.allowedValues == null) {
            alv = this.allowedValues;
        } else {
            alv = Lambda.intersect(this.allowedValues, that.allowedValues);
        }
        return new PlaybookParameterSpec(this.name, alv);
    }

    public boolean match(Map<String, ?> parameters) {
        if (!parameters.containsKey(name)) {
            return false;
        }
        if (allowedValues == null) {
            return true;
        }

        Object value = parameters.get(name);
        if (value instanceof PlaybookParameterSpec) {
            PlaybookParameterSpec spec = (PlaybookParameterSpec) value;
            // all values allowed by the target parameters are allowed by us
            return spec.allowedValues != null && !spec.allowedValues.isEmpty() && allowedValues.containsAll(spec.allowedValues);
        }
        return allowedValues.contains(parameters.get(name));
    }
}
