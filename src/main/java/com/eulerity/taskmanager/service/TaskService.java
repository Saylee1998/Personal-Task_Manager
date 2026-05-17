package com.eulerity.taskmanager.service;

import com.eulerity.taskmanager.dto.TaskAnalyticsResponse;
import com.eulerity.taskmanager.dto.TaskRequest;
import com.eulerity.taskmanager.dto.TaskResponse;
import com.eulerity.taskmanager.exception.TaskNotFoundException;
import com.eulerity.taskmanager.model.Priority;
import com.eulerity.taskmanager.model.Status;
import com.eulerity.taskmanager.model.Task;
import com.eulerity.taskmanager.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private static final Set<String> VALID_SORT_FIELDS =
            Set.of("dueDate", "priority", "title", "createdAt");

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public TaskResponse createTask(TaskRequest request) {
        if (!request.getDueDate().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Due date must be in the future");
        }

        Task task = new Task(
                request.getTitle(),
                request.getDescription(),
                request.getDueDate(),
                request.getPriority(),
                request.getStatus() != null ? request.getStatus() : Status.TODO
        );

        return toResponse(taskRepository.save(task));
    }

    public List<TaskResponse> getAllTasks() {
        return getAllTasks(null, null, null, null, null);
    }

    public List<TaskResponse> getAllTasks(Status status, Priority priority,
                                          String sortBy, String sortOrder, String search) {
        String effectiveSortBy = resolveSort(sortBy);

        String effectiveSortOrder = (sortOrder == null || sortOrder.isBlank())
                ? "desc" : sortOrder.toLowerCase();
        if (!Set.of("asc", "desc").contains(effectiveSortOrder)) {
            effectiveSortOrder = "desc";
        }
        boolean descending = "desc".equals(effectiveSortOrder);

        Comparator<Task> comparator = switch (effectiveSortBy) {
            case "dueDate"  -> Comparator.comparing(Task::getDueDate,
                                   Comparator.nullsLast(Comparator.naturalOrder()));
            case "priority" -> Comparator.comparingInt((Task t) -> t.getPriority().ordinal());
            case "title"    -> Comparator.comparing(Task::getTitle,
                                   String.CASE_INSENSITIVE_ORDER);
            default         -> Comparator.comparing(Task::getCreatedAt,
                                   Comparator.nullsLast(Comparator.naturalOrder()));
        };

        if (descending) {
            comparator = comparator.reversed();
        }

        return taskRepository.findAll().stream()
                .filter(task -> status   == null || task.getStatus()   == status)
                .filter(task -> priority == null || task.getPriority() == priority)
                .filter(task -> matchesSearch(task, search))
                .sorted(comparator)
                .map(this::toResponse)
                .toList();
    }

    public TaskAnalyticsResponse getAnalytics() {
        List<Task> tasks      = taskRepository.findAll();
        LocalDateTime now     = LocalDateTime.now();
        LocalDateTime in7Days = now.plusDays(7);

        Map<Status, Long> byStatus = new EnumMap<>(Status.class);
        for (Status s : Status.values()) byStatus.put(s, 0L);
        tasks.stream()
             .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()))
             .forEach(byStatus::put);

        Map<Priority, Long> byPriority = new EnumMap<>(Priority.class);
        for (Priority p : Priority.values()) byPriority.put(p, 0L);
        tasks.stream()
             .collect(Collectors.groupingBy(Task::getPriority, Collectors.counting()))
             .forEach(byPriority::put);

        long overdue = tasks.stream()
                .filter(t -> t.getDueDate() != null
                        && t.getDueDate().isBefore(now)
                        && t.getStatus() != Status.DONE)
                .count();

        long upcoming = tasks.stream()
                .filter(t -> t.getDueDate() != null
                        && !t.getDueDate().isBefore(now)
                        && !t.getDueDate().isAfter(in7Days)
                        && t.getStatus() != Status.DONE)
                .count();

        return new TaskAnalyticsResponse(tasks.size(), byStatus, byPriority, overdue, upcoming);
    }

    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        return toResponse(task);
    }

    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        boolean dueDateChanged = !request.getDueDate().equals(task.getDueDate());
        if (dueDateChanged && !request.getDueDate().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Due date must be in the future");
        }

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate());
        task.setPriority(request.getPriority());
        task.setStatus(request.getStatus() != null ? request.getStatus() : Status.TODO);

        return toResponse(taskRepository.save(task));
    }

    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        taskRepository.delete(task);
    }

    private static boolean matchesSearch(Task task, String search) {
        if (search == null || search.isBlank()) return true;
        String trimmed = search.trim();
        try {
            long id = Long.parseLong(trimmed);
            return task.getId() != null && task.getId().equals(id);
        } catch (NumberFormatException e) {
            return task.getTitle() != null &&
                   task.getTitle().toLowerCase().contains(trimmed.toLowerCase());
        }
    }

    private static String resolveSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return "createdAt";
        return VALID_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDueDate(),
                task.getPriority(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
