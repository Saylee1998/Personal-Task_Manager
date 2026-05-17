package com.eulerity.taskmanager.dto;

/**
 * Outbound DTO returned by the AI task suggestion endpoint.
 * Contains a structured task suggestion and the AI's reasoning.
 * The suggestion is not persisted; the client may use it to pre-fill a creation form.
 */
public class AITaskSuggestionResponse {

    private TaskRequest suggestedTask;
    private String reasoning;

    public AITaskSuggestionResponse() {}

    public AITaskSuggestionResponse(TaskRequest suggestedTask, String reasoning) {
        this.suggestedTask = suggestedTask;
        this.reasoning = reasoning;
    }

    public TaskRequest getSuggestedTask() {
        return suggestedTask;
    }

    public void setSuggestedTask(TaskRequest suggestedTask) {
        this.suggestedTask = suggestedTask;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
}
