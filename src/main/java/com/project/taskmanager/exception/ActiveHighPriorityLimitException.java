package com.project.taskmanager.exception;

/**
 * Thrown when creating or updating a task would exceed the maximum number of
 * active HIGH priority tasks. Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 */
public class ActiveHighPriorityLimitException extends RuntimeException {

    public ActiveHighPriorityLimitException(int limit) {
        super("Cannot exceed " + limit + " active HIGH priority tasks");
    }
}
