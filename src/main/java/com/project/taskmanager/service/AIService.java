package com.project.taskmanager.service;

import com.project.taskmanager.dto.AITaskSuggestionRequest;
import com.project.taskmanager.dto.AITaskSuggestionResponse;
import com.project.taskmanager.dto.TaskBreakdownResponse;
import com.project.taskmanager.dto.TaskResponse;
import org.springframework.stereotype.Service;

@Service
public class AIService {

    private final AIProvider aiProvider;

    public AIService(AIProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    public AITaskSuggestionResponse generateTaskSuggestion(AITaskSuggestionRequest request) {
        return aiProvider.suggestTask(request.getDescription());
    }

    public TaskBreakdownResponse breakdownTask(TaskResponse task) {
        return aiProvider.breakdownTask(task);
    }
}
