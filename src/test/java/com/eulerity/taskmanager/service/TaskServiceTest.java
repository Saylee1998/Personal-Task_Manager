package com.eulerity.taskmanager.service;

import com.eulerity.taskmanager.dto.TaskRequest;
import com.eulerity.taskmanager.dto.TaskResponse;
import com.eulerity.taskmanager.model.Priority;
import com.eulerity.taskmanager.model.Status;
import com.eulerity.taskmanager.model.Task;
import com.eulerity.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private LocalDateTime futureDate;
    private Task sampleTask;

    @BeforeEach
    void setUp() {
        futureDate = LocalDateTime.now().plusDays(30);
        sampleTask = new Task("Sample Task", "Sample description", futureDate, Priority.MEDIUM, Status.TODO);
        sampleTask.setId(1L);
    }

    @Test
    void createTask_HappyPath_ReturnsMappedResponse() {
        TaskRequest request = new TaskRequest("New Task", "Details", futureDate, Priority.HIGH, null);
        Task saved = new Task("New Task", "Details", futureDate, Priority.HIGH, Status.TODO);
        saved.setId(1L);

        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        TaskResponse response = taskService.createTask(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("New Task", response.getTitle());
        assertEquals(Priority.HIGH, response.getPriority());
        assertEquals(Status.TODO, response.getStatus());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void getAllTasks_HappyPath_ReturnsMappedList() {
        when(taskRepository.findAll()).thenReturn(List.of(sampleTask));

        List<TaskResponse> result = taskService.getAllTasks();

        assertEquals(1, result.size());
        assertEquals("Sample Task", result.get(0).getTitle());
        assertEquals(Priority.MEDIUM, result.get(0).getPriority());
        verify(taskRepository).findAll();
    }

    @Test
    void getTaskById_HappyPath_ReturnsMappedResponse() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));

        TaskResponse response = taskService.getTaskById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Sample Task", response.getTitle());
        verify(taskRepository).findById(1L);
    }

    @Test
    void updateTask_HappyPath_UpdatesAndReturnsMappedResponse() {
        LocalDateTime newDueDate = LocalDateTime.now().plusDays(60);
        TaskRequest request = new TaskRequest("Updated Task", "New desc", newDueDate, Priority.LOW, Status.IN_PROGRESS);
        Task updated = new Task("Updated Task", "New desc", newDueDate, Priority.LOW, Status.IN_PROGRESS);
        updated.setId(1L);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenReturn(updated);

        TaskResponse response = taskService.updateTask(1L, request);

        assertNotNull(response);
        assertEquals("Updated Task", response.getTitle());
        assertEquals(Priority.LOW, response.getPriority());
        assertEquals(Status.IN_PROGRESS, response.getStatus());
        verify(taskRepository).findById(1L);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void deleteTask_HappyPath_CallsRepositoryDelete() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));

        taskService.deleteTask(1L);

        verify(taskRepository).findById(1L);
        verify(taskRepository).delete(sampleTask);
    }
}
