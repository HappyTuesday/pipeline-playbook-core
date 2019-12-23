package com.yit.deploy.core.info;

public class UserParameterInfo {
    private String type;
    private String description;
    private boolean required;
    private boolean hidden;
    private boolean persistent;
    private VariableInfo choices;
    private Integer order;
    private VariableInfo options;

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

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public VariableInfo getChoices() {
        return choices;
    }

    public void setChoices(VariableInfo choices) {
        this.choices = choices;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public VariableInfo getOptions() {
        return options;
    }

    public void setOptions(VariableInfo options) {
        this.options = options;
    }
}
