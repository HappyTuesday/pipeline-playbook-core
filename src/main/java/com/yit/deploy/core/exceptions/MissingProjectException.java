package com.yit.deploy.core.exceptions;

public class MissingProjectException extends RuntimeException {

    public final String projectName;

    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param projectName the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public MissingProjectException(String projectName) {
        super("project " + projectName + " could not be found");
        this.projectName = projectName;
    }
}
