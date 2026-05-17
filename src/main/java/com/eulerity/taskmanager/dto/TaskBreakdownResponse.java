package com.eulerity.taskmanager.dto;

import java.util.List;

public class TaskBreakdownResponse {

    private TaskResponse originalTask;
    private List<SubtaskSuggestion> suggestedSubtasks;
    private String reasoning;

    public TaskBreakdownResponse() {}

    public TaskBreakdownResponse(TaskResponse originalTask,
                                  List<SubtaskSuggestion> suggestedSubtasks,
                                  String reasoning) {
        this.originalTask      = originalTask;
        this.suggestedSubtasks = suggestedSubtasks;
        this.reasoning         = reasoning;
    }

    public TaskResponse getOriginalTask()                        { return originalTask; }
    public void setOriginalTask(TaskResponse originalTask)       { this.originalTask = originalTask; }

    public List<SubtaskSuggestion> getSuggestedSubtasks()                          { return suggestedSubtasks; }
    public void setSuggestedSubtasks(List<SubtaskSuggestion> suggestedSubtasks)    { this.suggestedSubtasks = suggestedSubtasks; }

    public String getReasoning()                 { return reasoning; }
    public void setReasoning(String reasoning)   { this.reasoning = reasoning; }
}
