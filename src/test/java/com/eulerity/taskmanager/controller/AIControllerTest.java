package com.eulerity.taskmanager.controller;

import com.eulerity.taskmanager.dto.SubtaskSuggestion;
import com.eulerity.taskmanager.dto.TaskBreakdownResponse;
import com.eulerity.taskmanager.dto.TaskResponse;
import com.eulerity.taskmanager.exception.TaskNotFoundException;
import com.eulerity.taskmanager.model.Priority;
import com.eulerity.taskmanager.model.Status;
import com.eulerity.taskmanager.service.AIService;
import com.eulerity.taskmanager.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "gemini.api.key=test-key")
class AIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    //@MockBean
    @MockitoBean
    private AIService aiService;

    //@MockBean
    @MockitoBean
    private TaskService taskService;

    @Test
    void breakdownTask_Returns200WithBreakdown() throws Exception {
        Long id = 1L;
        LocalDateTime due = LocalDateTime.now().plusDays(30);
        TaskResponse task = new TaskResponse(id, "Complex Task", "Do many things",
                due, Priority.HIGH, Status.TODO, due, due);

        List<SubtaskSuggestion> subtasks = List.of(
                new SubtaskSuggestion("Step 1", "First step",  Priority.HIGH,   Status.TODO),
                new SubtaskSuggestion("Step 2", "Second step", Priority.MEDIUM, Status.TODO)
        );
        TaskBreakdownResponse breakdown = new TaskBreakdownResponse(task, subtasks, "Two clear steps needed.");

        when(taskService.getTaskById(id)).thenReturn(task);
        when(aiService.breakdownTask(task)).thenReturn(breakdown);

        mockMvc.perform(post("/tasks/{id}/breakdown", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalTask.id").value(id))
                .andExpect(jsonPath("$.originalTask.title").value("Complex Task"))
                .andExpect(jsonPath("$.suggestedSubtasks").isArray())
                .andExpect(jsonPath("$.suggestedSubtasks.length()").value(2))
                .andExpect(jsonPath("$.suggestedSubtasks[0].title").value("Step 1"))
                .andExpect(jsonPath("$.reasoning").value("Two clear steps needed."));
    }

    @Test
    void breakdownTask_TaskNotFound_Returns404() throws Exception {
        when(taskService.getTaskById(999L)).thenThrow(new TaskNotFoundException(999L));

        mockMvc.perform(post("/tasks/999/breakdown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void breakdownTask_AIServiceError_Returns500() throws Exception {
        Long id = 2L;
        LocalDateTime due = LocalDateTime.now().plusDays(30);
        TaskResponse task = new TaskResponse(id, "Complex Task", null,
                due, Priority.MEDIUM, Status.TODO, due, due);

        when(taskService.getTaskById(id)).thenReturn(task);
        when(aiService.breakdownTask(task)).thenThrow(new RuntimeException("AI service unavailable"));

        mockMvc.perform(post("/tasks/{id}/breakdown", id))
                .andExpect(status().isInternalServerError());
    }
}
