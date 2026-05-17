package com.eulerity.taskmanager.service;

import com.eulerity.taskmanager.dto.AITaskSuggestionRequest;
import com.eulerity.taskmanager.dto.AITaskSuggestionResponse;
import com.eulerity.taskmanager.dto.TaskBreakdownResponse;
import com.eulerity.taskmanager.dto.TaskResponse;
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
