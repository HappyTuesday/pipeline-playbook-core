package com.yit.deploy.core.exceptions;

public class DeployFailureException extends RuntimeException {

    private final String failureType;
    private final Object detailedReason;

    public Object getDetailedReason() {
        return detailedReason;
    }

    public String getFailureType() {
        return failureType;
    }

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @since 1.4
     */
    public DeployFailureException(String failureType, String message) {
        super(message);
        this.failureType = failureType;
        this.detailedReason = null;
    }

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @since 1.4
     */
    public DeployFailureException(String failureType, Object detailedReason, String message) {
        super(message);
        this.failureType = failureType;
        this.detailedReason = detailedReason;
    }

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A <tt>null</tt> value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     * @since 1.4
     */
    public DeployFailureException(String failureType, Object detailedReason, String message, Throwable cause) {
        super(message, cause);
        this.failureType = failureType;
        this.detailedReason = detailedReason;
    }
}
