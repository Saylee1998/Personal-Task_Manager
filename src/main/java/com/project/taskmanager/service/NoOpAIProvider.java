package com.project.taskmanager.service;

import com.project.taskmanager.dto.AITaskSuggestionResponse;
import com.project.taskmanager.dto.TaskBreakdownResponse;
import com.project.taskmanager.dto.TaskRequest;
import com.project.taskmanager.dto.TaskResponse;
import com.project.taskmanager.model.Priority;
import com.project.taskmanager.model.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@ConditionalOnExpression("'${gemini.api.key:}'.isEmpty()")
public class NoOpAIProvider implements AIProvider {

    @Override
    public AITaskSuggestionResponse suggestTask(String description) {
        TaskRequest suggestion = new TaskRequest(
                description,
                null,
                LocalDateTime.now().plusDays(1),
                Priority.MEDIUM,
                Status.TODO
        );
        return new AITaskSuggestionResponse(suggestion,
                "AI provider is not configured. Set GEMINI_API_KEY environment variable to enable AI-powered suggestions.");
    }

    @Override
    public TaskBreakdownResponse breakdownTask(TaskResponse task) {
        return new TaskBreakdownResponse(task, List.of(),
                "AI provider is not configured. Set GEMINI_API_KEY environment variable to enable AI-powered task breakdowns.");
    }
}
