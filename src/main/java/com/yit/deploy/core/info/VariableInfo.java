package com.yit.deploy.core.info;

import com.google.gson.Gson;
import com.yit.deploy.core.model.VariableName;
import com.yit.deploy.core.model.VariableType;
import com.yit.deploy.core.variables.variable.Variable;
import groovy.lang.Closure;

import java.util.List;
import java.util.Map;

public class VariableInfo {

    private String name;
    private String id;
    private VariableType type;
    private ClosureInfo closure;
    private Object value;
    private VariableInfo variable;
    private List<VariableInfo> list;
    private Map<String, VariableInfo> map;
    private Object option;

    public VariableInfo() {
    }

    public VariableInfo(VariableInfo that) {
        this.name = that.name;
        this.id = that.id;
        this.type = that.type;
        this.closure = that.closure;
        this.value = that.value;
        this.variable = that.variable;
        this.list = that.list;
        this.map = that.map;
        this.option = that.option;
    }

    public String toJson() {
        return DeployTableResponse.GSON.toJson(this);
    }

    public static VariableInfo fromJson(String json) {
        if (json == null) {
            return null;
        }
        return DeployTableResponse.GSON.fromJson(json, VariableInfo.class);
    }

    public VariableInfo(VariableName name, String id, VariableType type) {
        this.name = name == null ? null : name.toString();
        this.id = id;
        this.type = type;
    }

    public VariableInfo(VariableName name, String id, VariableType type, List<VariableInfo> list) {
        this(name, id, type);
        this.list = list;
    }

    public VariableInfo(VariableName name, String id, VariableType type, Map<String, VariableInfo> map) {
        this(name, id, type);
        this.map = map;
    }

    public VariableInfo(VariableName name, String id, VariableType type, ClosureInfo closure) {
        this(name, id, type);
        this.closure = closure;
    }

    public VariableInfo(VariableName name, String id, VariableType type, VariableInfo variable) {
        this(name, id, type);
        this.variable = variable;
    }

    public VariableInfo(VariableName name, String id, VariableType type, VariableInfo variable, ClosureInfo closure) {
        this(name, id, type);
        this.variable = variable;
        this.closure = closure;
    }

    public VariableInfo(VariableName name, String id, VariableType type, Object value) {
        this(name, id, type);
        this.value = value;
    }

    public VariableName getVariableName() {
        return name == null ? null : VariableName.parse(name);
    }

    public <T> Variable<T> toVariable(Class<T> clazz) {
        return (Variable<T>) toVariable();
    }

    public Variable toVariable() {
        return type.toVariable(this);
    }

    public static VariableInfo parse(String text) {
        return new Gson().fromJson(text, VariableInfo.class);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VariableType getType() {
        return type;
    }

    public void setType(VariableType type) {
        this.type = type;
    }

    public ClosureInfo getClosure() {
        return closure;
    }

    public void setClosure(ClosureInfo closure) {
        this.closure = closure;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public VariableInfo getVariable() {
        return variable;
    }

    public void setVariable(VariableInfo variable) {
        this.variable = variable;
    }

    public List<VariableInfo> getList() {
        return list;
    }

    public void setList(List<VariableInfo> list) {
        this.list = list;
    }

    public Map<String, VariableInfo> getMap() {
        return map;
    }

    public void setMap(Map<String, VariableInfo> map) {
        this.map = map;
    }

    public Object getOption() {
        return option;
    }

    public void setOption(Object option) {
        this.option = option;
    }
}
