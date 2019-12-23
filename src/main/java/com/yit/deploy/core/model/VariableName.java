package com.yit.deploy.core.model;

import com.yit.deploy.core.function.Lambda;

import java.util.Arrays;

public class VariableName {

    public final String REPEATABLE_NAME = "*";

    public final String[] path;

    public VariableName(String[] path) {
        this.path = path;
    }

    public String first() {
        return path[0];
    }

    public String last() {
        return path[path.length - 1];
    }

    public VariableName field(String fieldName) {
        if (fieldName == null || fieldName.isEmpty() || REPEATABLE_NAME.equals(fieldName)) {
            throw new IllegalArgumentException("invalid field name " + fieldName);
        }

        return new VariableName(Lambda.append(path, fieldName));
    }

    public boolean repeatable() {
        return REPEATABLE_NAME.equals(last());
    }

    public VariableName toRepeatable() {
        return new VariableName(Lambda.append(path, REPEATABLE_NAME));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(path);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VariableName && Arrays.equals(path, ((VariableName) obj).path);
    }

    @Override
    public String toString() {
        return path.length == 1 ? path[0] : String.join(".", path);
    }

    public static VariableName parse(String name) {
        return new VariableName(name.split("\\."));
    }
}
