package com.eulerity.taskmanager.exception;

/**
 * Thrown when a requested task does not exist in the database.
 * Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(Long id) {
        super("Task not found with id: " + id);
    }
}
