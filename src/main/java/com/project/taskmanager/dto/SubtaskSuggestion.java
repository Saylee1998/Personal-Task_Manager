package com.project.taskmanager.dto;

import com.project.taskmanager.model.Priority;
import com.project.taskmanager.model.Status;

public class SubtaskSuggestion {

    private String title;
    private String description;
    private Priority priority;
    private Status status;

    public SubtaskSuggestion() {}

    public SubtaskSuggestion(String title, String description, Priority priority, Status status) {
        this.title       = title;
        this.description = description;
        this.priority    = priority;
        this.status      = status != null ? status : Status.TODO;
    }

    public String getTitle()              { return title; }
    public void setTitle(String title)    { this.title = title; }

    public String getDescription()                   { return description; }
    public void setDescription(String description)   { this.description = description; }

    public Priority getPriority()                 { return priority; }
    public void setPriority(Priority priority)    { this.priority = priority; }

    public Status getStatus()              { return status; }
    public void setStatus(Status status)   { this.status = status != null ? status : Status.TODO; }
}
