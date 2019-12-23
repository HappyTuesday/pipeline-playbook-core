package com.yit.deploy.core.exceptions;

import hudson.AbortException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * use this exception to enclose the AbortException
 */
public class ExitException extends RuntimeException {
    public ExitException(@Nonnull String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    public ExitException(@Nonnull Throwable cause) {
        this(cause.getMessage(), cause);
    }

    public ExitException(@Nonnull String message) {
        this(message, null);
    }

    public static ExitException wrap(Throwable t) {
        if (t instanceof ExitException) return (ExitException) t;
        return new ExitException(t);
    }

    public static boolean belongsTo(Throwable t) {
        return belongsTo(t, 6);
    }

    private static boolean belongsTo(Throwable t, int level) {
        if (level < 0) return false;
        if (t == null) return false;
        if (t instanceof ExitException || t instanceof AbortException || t instanceof InterruptedException) {
            return true;
        }
        if (t == t.getCause()) return false;
        return belongsTo(t.getCause(), level - 1);
    }

    public static AbortException firstAbort(Throwable t) {
        return search(t, AbortException.class, 6);
    }

    public static InterruptedException firstInterrupted(Throwable t) {
        return search(t, InterruptedException.class, 6);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T search(Throwable t, Class<T> target, int level) {
        if (level < 0) return null;
        if (t == null) return null;
        if (target.isInstance(t)) return (T) t;
        if (t == t.getCause()) return null;
        return search(t.getCause(), target, level - 1);
    }
}
