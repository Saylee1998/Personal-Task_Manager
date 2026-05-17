package com.eulerity.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound DTO for the AI task suggestion endpoint.
 * Contains a plain-language description that the AI provider interprets
 * into a structured task suggestion.
 */
public class AITaskSuggestionRequest {

    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    public AITaskSuggestionRequest() {}

    public AITaskSuggestionRequest(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
