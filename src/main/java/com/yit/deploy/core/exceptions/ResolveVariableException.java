package com.yit.deploy.core.exceptions;


import java.util.LinkedList;
import java.util.List;

/**
 * Created by nick on 12/09/2017.
 */
public class ResolveVariableException extends RuntimeException {
    private final String variableName;
    private final LinkedList<String> chain;

    public ResolveVariableException(String variableName, String message) {
        this(variableName, message, null);
    }

    public ResolveVariableException(String variableName, String message, Exception e) {
        super(prepareErrorMessage(variableName, message, e), e);
        this.variableName = variableName;
        this.chain = new LinkedList<>();
        this.chain.add(variableName);
    }

    private static String prepareErrorMessage(String variableName, String message, Exception e) {
        StringBuilder sb = new StringBuilder();
        if (message != null && !message.isEmpty()) {
            sb.append(message).append(". ");
        }
        sb.append("Resolve variable [").append(variableName).append("] value failed. ");
        String innerMessage = e == null ? null : e.getMessage();
        if (innerMessage != null && !innerMessage.isEmpty()) {
            sb.append(" Detailed error: ").append(innerMessage).append(". ");
        }
        return sb.toString();
    }

    public String getVariableName() {
        return variableName;
    }

    @Override
    public String toString() {
        return super.toString() + ". variable resolving chain is " + chain + ".";
    }

    public LinkedList<String> getChain() {
        return chain;
    }
}
