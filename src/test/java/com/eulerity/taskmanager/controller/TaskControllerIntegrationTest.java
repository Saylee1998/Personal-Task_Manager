package com.eulerity.taskmanager.controller;

import com.eulerity.taskmanager.dto.TaskRequest;
import com.eulerity.taskmanager.model.Priority;
import com.eulerity.taskmanager.model.Status;
import com.eulerity.taskmanager.model.Task;
import com.eulerity.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "gemini.api.key=test-key")
class TaskControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TaskRepository taskRepository;

    private LocalDateTime futureDate;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        futureDate = LocalDateTime.now().plusDays(30).withNano(0);
    }

    @Test
    void createTask_Returns201WithBody() {
        TaskRequest request = new TaskRequest("Integration Task", "Description", futureDate, Priority.HIGH, null);

        ResponseEntity<Map> response = restTemplate.postForEntity("/tasks", request, Map.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("id"));
        assertEquals("Integration Task", response.getBody().get("title"));
        assertEquals("HIGH", response.getBody().get("priority"));
        assertEquals("TODO", response.getBody().get("status"));
    }

    @Test
    void getAllTasks_Returns200WithList() {
        TaskRequest request = new TaskRequest("List Task", null, futureDate, Priority.LOW, null);
        restTemplate.postForEntity("/tasks", request, Map.class);

        ResponseEntity<List> response = restTemplate.getForEntity("/tasks", List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getTaskById_Returns200WithTask() {
        TaskRequest request = new TaskRequest("Single Task", null, futureDate, Priority.MEDIUM, null);
        ResponseEntity<Map> created = restTemplate.postForEntity("/tasks", request, Map.class);
        Number id = (Number) created.getBody().get("id");

        ResponseEntity<Map> response = restTemplate.getForEntity("/tasks/" + id.longValue(), Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Single Task", response.getBody().get("title"));
        assertEquals(id.intValue(), response.getBody().get("id"));
    }

    @Test
    void updateTask_Returns200WithUpdatedData() {
        TaskRequest create = new TaskRequest("Original Task", null, futureDate, Priority.LOW, null);
        ResponseEntity<Map> created = restTemplate.postForEntity("/tasks", create, Map.class);
        Number id = (Number) created.getBody().get("id");

        TaskRequest update = new TaskRequest("Updated Task", "New desc", futureDate.plusDays(10), Priority.HIGH, null);
        HttpEntity<TaskRequest> updateEntity = new HttpEntity<>(update);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/tasks/" + id.longValue(), HttpMethod.PUT, updateEntity, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Updated Task", response.getBody().get("title"));
        assertEquals("HIGH", response.getBody().get("priority"));
    }

    @Test
    void deleteTask_Returns204() {
        TaskRequest request = new TaskRequest("Task to Delete", null, futureDate, Priority.LOW, null);
        ResponseEntity<Map> created = restTemplate.postForEntity("/tasks", request, Map.class);
        Number id = (Number) created.getBody().get("id");

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/tasks/" + id.longValue(), HttpMethod.DELETE, null, Void.class);

        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());
    }

    @Test
    void getTaskById_NonExistentId_Returns404WithMessage() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/tasks/999", Map.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().get("status"));
        assertTrue(((String) response.getBody().get("message")).contains("999"));
    }

    @Test
    void createTask_BlankTitle_Returns400WithFieldError() {
        TaskRequest request = new TaskRequest("", "Description", futureDate, Priority.HIGH, null);

        ResponseEntity<Map> response = restTemplate.postForEntity("/tasks", request, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        Map<String, String> errors = (Map<String, String>) response.getBody().get("errors");
        assertNotNull(errors);
        assertTrue(errors.containsKey("title"));
    }

    @Test
    void getAllTasks_FilterByStatus_ReturnsOnlyMatchingTasks() {
        restTemplate.postForEntity("/tasks",
            new TaskRequest("Todo Task", null, futureDate, Priority.MEDIUM, Status.TODO), Map.class);
        restTemplate.postForEntity("/tasks",
            new TaskRequest("Done Task", null, futureDate, Priority.LOW, Status.DONE), Map.class);

        ResponseEntity<List> response = restTemplate.getForEntity("/tasks?status=TODO", List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        Map<String, Object> task = (Map<String, Object>) response.getBody().get(0);
        assertEquals("TODO", task.get("status"));
        assertEquals("Todo Task", task.get("title"));
    }

    @Test
    void getAllTasks_SortByDueDateAscending_ReturnsInChronologicalOrder() {
        LocalDateTime nearDate = futureDate;
        LocalDateTime farDate  = futureDate.plusDays(60);
        restTemplate.postForEntity("/tasks",
            new TaskRequest("Far Task",  null, farDate,  Priority.LOW, null), Map.class);
        restTemplate.postForEntity("/tasks",
            new TaskRequest("Near Task", null, nearDate, Priority.LOW, null), Map.class);

        ResponseEntity<List> response = restTemplate.getForEntity(
            "/tasks?sortBy=dueDate&sortOrder=asc", List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertEquals("Near Task", ((Map<?, ?>) response.getBody().get(0)).get("title"));
        assertEquals("Far Task",  ((Map<?, ?>) response.getBody().get(1)).get("title"));
    }

    @Test
    void getAllTasks_SearchByTitle_ReturnsMatchingTasks() {
        restTemplate.postForEntity("/tasks",
            new TaskRequest("Meeting notes", null, futureDate, Priority.MEDIUM, null), Map.class);
        restTemplate.postForEntity("/tasks",
            new TaskRequest("Deploy release", null, futureDate, Priority.HIGH, null), Map.class);

        ResponseEntity<List> response = restTemplate.getForEntity("/tasks?search=meeting", List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Meeting notes", ((Map<?, ?>) response.getBody().get(0)).get("title"));
    }

    @Test
    void getAllTasks_SearchById_ReturnsExactTask() {
        ResponseEntity<Map> created = restTemplate.postForEntity("/tasks",
            new TaskRequest("Specific Task", null, futureDate, Priority.LOW, null), Map.class);
        Number id = (Number) created.getBody().get("id");

        ResponseEntity<List> response = restTemplate.getForEntity(
            "/tasks?search=" + id.longValue(), List.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Specific Task", ((Map<?, ?>) response.getBody().get(0)).get("title"));
    }

    @Test
    void getAnalytics_EmptyDatabase_ReturnsZeroCountsWithAllEnumKeys() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/tasks/analytics", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(0, ((Number) body.get("totalTasks")).intValue());
        assertEquals(0, ((Number) body.get("overdueTasks")).intValue());
        assertEquals(0, ((Number) body.get("upcomingTasksNext7Days")).intValue());

        Map<String, Number> byStatus = (Map<String, Number>) body.get("byStatus");
        assertNotNull(byStatus);
        assertEquals(0, byStatus.get("TODO").intValue());
        assertEquals(0, byStatus.get("IN_PROGRESS").intValue());
        assertEquals(0, byStatus.get("DONE").intValue());

        Map<String, Number> byPriority = (Map<String, Number>) body.get("byPriority");
        assertNotNull(byPriority);
        assertEquals(0, byPriority.get("LOW").intValue());
        assertEquals(0, byPriority.get("MEDIUM").intValue());
        assertEquals(0, byPriority.get("HIGH").intValue());
    }

    @Test
    void getAnalytics_MixedTasks_ReturnsCorrectCounts() {
        restTemplate.postForEntity("/tasks", new TaskRequest("T1", null, futureDate, Priority.HIGH,   Status.TODO),        Map.class);
        restTemplate.postForEntity("/tasks", new TaskRequest("T2", null, futureDate, Priority.HIGH,   Status.IN_PROGRESS), Map.class);
        restTemplate.postForEntity("/tasks", new TaskRequest("T3", null, futureDate, Priority.MEDIUM, Status.DONE),        Map.class);
        restTemplate.postForEntity("/tasks", new TaskRequest("T4", null, futureDate, Priority.LOW,    Status.TODO),        Map.class);

        ResponseEntity<Map> response = restTemplate.getForEntity("/tasks/analytics", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertEquals(4, ((Number) body.get("totalTasks")).intValue());

        Map<String, Number> byStatus = (Map<String, Number>) body.get("byStatus");
        assertEquals(2, byStatus.get("TODO").intValue());
        assertEquals(1, byStatus.get("IN_PROGRESS").intValue());
        assertEquals(1, byStatus.get("DONE").intValue());

        Map<String, Number> byPriority = (Map<String, Number>) body.get("byPriority");
        assertEquals(1, byPriority.get("LOW").intValue());
        assertEquals(1, byPriority.get("MEDIUM").intValue());
        assertEquals(2, byPriority.get("HIGH").intValue());
    }

    @Test
    void getAnalytics_OnlyTodoTasks_OtherStatusesShowZero() {
        restTemplate.postForEntity("/tasks", new TaskRequest("T1", null, futureDate, Priority.MEDIUM, Status.TODO), Map.class);
        restTemplate.postForEntity("/tasks", new TaskRequest("T2", null, futureDate, Priority.MEDIUM, Status.TODO), Map.class);

        ResponseEntity<Map> response = restTemplate.getForEntity("/tasks/analytics", Map.class);

        Map<String, Number> byStatus = (Map<String, Number>) response.getBody().get("byStatus");
        assertEquals(2, byStatus.get("TODO").intValue());
        assertEquals(0, byStatus.get("IN_PROGRESS").intValue());
        assertEquals(0, byStatus.get("DONE").intValue());
    }

    @Test
    void getAnalytics_OverdueTasks_CountsNonDoneOnly() {
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1).withNano(0);
        taskRepository.save(new Task("Overdue Todo",        null, pastDate, Priority.HIGH,   Status.TODO));
        taskRepository.save(new Task("Overdue In Progress", null, pastDate, Priority.MEDIUM, Status.IN_PROGRESS));
        taskRepository.save(new Task("Overdue Done",        null, pastDate, Priority.LOW,    Status.DONE));

        ResponseEntity<Map> response = restTemplate.getForEntity("/tasks/analytics", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, ((Number) response.getBody().get("overdueTasks")).intValue());
    }

    @Test
    void getAnalytics_TaskDueExactly7DaysFromNow_CountsAsUpcoming() {
        LocalDateTime exactly7Days = LocalDateTime.now().plusDays(7).withNano(0);
        restTemplate.postForEntity("/tasks",
            new TaskRequest("Boundary Task", null, exactly7Days, Priority.MEDIUM, Status.TODO), Map.class);

        ResponseEntity<Map> response = restTemplate.getForEntity("/tasks/analytics", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, ((Number) response.getBody().get("upcomingTasksNext7Days")).intValue());
    }

    @Test
    void getAnalytics_UpcomingTasks_CountsWithin7DaysNonDoneOnly() {
        LocalDateTime in3Days  = LocalDateTime.now().plusDays(3).withNano(0);
        LocalDateTime in10Days = LocalDateTime.now().plusDays(10).withNano(0);

        restTemplate.postForEntity("/tasks", new TaskRequest("Upcoming",      null, in3Days,  Priority.MEDIUM, Status.TODO), Map.class);
        restTemplate.postForEntity("/tasks", new TaskRequest("Far Future",    null, in10Days, Priority.MEDIUM, Status.TODO), Map.class);
        restTemplate.postForEntity("/tasks", new TaskRequest("Done Upcoming", null, in3Days,  Priority.MEDIUM, Status.DONE), Map.class);

        ResponseEntity<Map> response = restTemplate.getForEntity("/tasks/analytics", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, ((Number) response.getBody().get("upcomingTasksNext7Days")).intValue());
    }
}
