package com.yit.deploy.core.exceptions;

import java.util.Map;

public class ParallelExecutionException extends RuntimeException {
    private final Map<String, Throwable> branchErrors;

    public ParallelExecutionException(Map<String, Throwable> branchErrors) {
        super("execution failed in branches [" + String.join(", ", branchErrors.keySet()) + "]");
        this.branchErrors = branchErrors;
    }

    public Map<String, Throwable> getBranchErrors() {
        return branchErrors;
    }
}
