package com.project.taskmanager.dto;

import com.project.taskmanager.model.Priority;
import com.project.taskmanager.model.Status;

import java.time.LocalDateTime;

/**
 * Immutable outbound DTO returned to the client for all task endpoints.
 * Mirrors the Task entity fields but decouples the API contract
 * from the persistence model. Jackson serializes via getters;
 * no setters or no-args constructor are needed for outbound-only use.
 */
public class TaskResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final LocalDateTime dueDate;
    private final Priority priority;
    private final Status status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public TaskResponse(Long id, String title, String description, LocalDateTime dueDate,
                        Priority priority, Status status,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public Priority getPriority() {
        return priority;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
