package com.yit.deploy.core.model;

import com.yit.deploy.core.dsl.evaluate.JobEvaluationContext;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.variables.Variables;
import com.yit.deploy.core.variables.variable.UserParameterVariable;
import com.yit.deploy.core.variables.variable.Variable;

import java.util.*;

public class ProjectParameter {
    private String parameterName;
    private String type;
    private Object defaultValue;
    private String description;
    private boolean hidden;
    private boolean persistent;
    private List choices;
    private Integer order;
    private boolean required;
    private Map<String, Object> options;

    public void merge(ProjectParameter p) {
        type = p.type;
        defaultValue = p.defaultValue;
        description = Lambda.cascade(p.description, description);
        hidden = hidden || p.hidden;
        persistent = persistent || p.persistent;
        choices = Lambda.cascade(p.choices, choices);
        order = Lambda.cascade(p.order, order);
        options = Lambda.cascade(p.options, options);
    }

    public static void mergeList(List<ProjectParameter> target, List<ProjectParameter> source) {
        for (ProjectParameter p : source) {
            int index = Lambda.findIndexOf(target, pp -> pp.parameterName.equals(p.parameterName));
            if (index < 0) {
                target.add(p);
            } else {
                target.get(index).merge(p);
            }
        }
    }

    public static void sortList(List<ProjectParameter> ls) {
        ls.sort(Comparator.comparingInt(a -> Lambda.cascade(a.order, Integer.MAX_VALUE)));
    }

    public static List<ProjectParameter> getParameters(Variables vars, JobEvaluationContext context) {
        List<ProjectParameter> ps = new ArrayList<>();
        for (Iterator<Variable> iter = vars.allVariables(); iter.hasNext();) {
            Variable v = iter.next();
            if (v instanceof UserParameterVariable) {
                UserParameterVariable up = (UserParameterVariable) v;
                ProjectParameter pp = up.toProjectParameter(context);
                int index = Lambda.findIndexOf(ps, p -> Objects.equals(p.parameterName, pp.parameterName));
                if (index < 0) {
                    ps.add(pp);
                } else {
                    ps.set(index, pp);
                }
            }
        }
        return ps;
    }

    @Override
    public String toString() {
        return parameterName + "(" + defaultValue + ")";
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public List getChoices() {
        return choices;
    }

    public void setChoices(List choices) {
        this.choices = choices;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Map<String, Object> getOptions() { return options; }

    public void setOptions(Map<String, Object> options) { this.options = options; }
}