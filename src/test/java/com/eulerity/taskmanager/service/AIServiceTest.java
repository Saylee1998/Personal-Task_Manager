package com.eulerity.taskmanager.service;

import com.eulerity.taskmanager.dto.AITaskSuggestionRequest;
import com.eulerity.taskmanager.dto.AITaskSuggestionResponse;
import com.eulerity.taskmanager.dto.TaskBreakdownResponse;
import com.eulerity.taskmanager.dto.TaskRequest;
import com.eulerity.taskmanager.dto.TaskResponse;
import com.eulerity.taskmanager.model.Priority;
import com.eulerity.taskmanager.model.Status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIServiceTest {

    @Mock
    private AIProvider aiProvider;

    @InjectMocks
    private AIService aiService;

    @Test
    void generateTaskSuggestion_DelegatesToProviderWithCorrectDescription() {
        String description = "Write unit tests for the API";
        AITaskSuggestionRequest request = new AITaskSuggestionRequest(description);

        TaskRequest suggestedTask = new TaskRequest(
                "Write API Tests", "Cover all service methods",
                LocalDateTime.now().plusDays(7), Priority.HIGH, Status.TODO);
        AITaskSuggestionResponse expected = new AITaskSuggestionResponse(suggestedTask, "Testing ensures reliability");

        when(aiProvider.suggestTask(description)).thenReturn(expected);

        AITaskSuggestionResponse result = aiService.generateTaskSuggestion(request);

        assertNotNull(result);
        assertSame(expected, result);
        assertEquals("Testing ensures reliability", result.getReasoning());
        assertEquals("Write API Tests", result.getSuggestedTask().getTitle());
        verify(aiProvider).suggestTask(description);
    }

    @Test
    void breakdownTask_DelegatesToProviderWithCorrectTask() {
        LocalDateTime due = LocalDateTime.now().plusDays(30);
        TaskResponse task = new TaskResponse(1L, "Complex Task", null, due, Priority.HIGH, Status.TODO, due, due);
        TaskBreakdownResponse expected = new TaskBreakdownResponse(task, List.of(), "reasoning");

        when(aiProvider.breakdownTask(task)).thenReturn(expected);

        TaskBreakdownResponse result = aiService.breakdownTask(task);

        assertSame(expected, result);
        verify(aiProvider).breakdownTask(task);
    }
}
