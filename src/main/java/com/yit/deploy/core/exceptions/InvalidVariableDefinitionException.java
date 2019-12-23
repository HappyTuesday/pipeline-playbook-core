package com.yit.deploy.core.exceptions;

public class InvalidVariableDefinitionException extends RuntimeException {
    private final String name;

    public InvalidVariableDefinitionException(String name, String message) {
        super("definition of " + name + " is invalid" + (message == null ? "" : ": " + message));
        this.name = name;
    }

    public InvalidVariableDefinitionException(String name) {
        this(name, name);
    }

    public String getName() {
        return name;
    }
}
