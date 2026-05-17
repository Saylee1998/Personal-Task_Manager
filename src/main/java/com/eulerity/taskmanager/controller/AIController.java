package com.eulerity.taskmanager.controller;

import com.eulerity.taskmanager.dto.AITaskSuggestionRequest;
import com.eulerity.taskmanager.dto.AITaskSuggestionResponse;
import com.eulerity.taskmanager.dto.TaskBreakdownResponse;
import com.eulerity.taskmanager.service.AIService;
import com.eulerity.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
public class AIController {

    private final AIService   aiService;
    private final TaskService taskService;

    public AIController(AIService aiService, TaskService taskService) {
        this.aiService   = aiService;
        this.taskService = taskService;
    }

    @PostMapping("/suggest")
    public ResponseEntity<AITaskSuggestionResponse> suggestTask(
            @Valid @RequestBody AITaskSuggestionRequest request) {
        return ResponseEntity.ok(aiService.generateTaskSuggestion(request));
    }

    @PostMapping("/{id}/breakdown")
    public ResponseEntity<TaskBreakdownResponse> breakdownTask(@PathVariable Long id) {
        return ResponseEntity.ok(aiService.breakdownTask(taskService.getTaskById(id)));
    }
}
