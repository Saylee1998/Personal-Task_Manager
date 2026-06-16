package com.project.taskmanager.controller;

import com.project.taskmanager.dto.TaskAnalyticsResponse;
import com.project.taskmanager.model.Priority;
import com.project.taskmanager.model.Status;
import com.project.taskmanager.service.TaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TaskViewController {

    private final TaskService taskService;
    private final int maxActiveHighPriority;

    public TaskViewController(TaskService taskService,
                              @Value("${task.high-priority.max-active:3}") int maxActiveHighPriority) {
        this.taskService = taskService;
        this.maxActiveHighPriority = maxActiveHighPriority;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        TaskAnalyticsResponse analytics = taskService.getAnalytics();
        long activeHighCount = taskService.countActiveHighPriorityTasks();
        long highPct = maxActiveHighPriority > 0
                ? Math.min(100, activeHighCount * 100 / maxActiveHighPriority)
                : 0;

        model.addAttribute("tasks",            taskService.getAllTasks());
        model.addAttribute("totalTasks",       analytics.getTotalTasks());
        model.addAttribute("overdueTasks",     analytics.getOverdueTasks());
        model.addAttribute("doneTasks",        analytics.getByStatus().getOrDefault(Status.DONE, 0L));
        model.addAttribute("inProgressTasks",  analytics.getByStatus().getOrDefault(Status.IN_PROGRESS, 0L));
        model.addAttribute("activeHighCount",  activeHighCount);
        model.addAttribute("maxActiveHigh",    maxActiveHighPriority);
        model.addAttribute("highPct",          highPct);
        return "index";
    }

    @GetMapping("/tasks/analytics")
    public String analyticsPage(Model model) {
        TaskAnalyticsResponse analytics = taskService.getAnalytics();
        long activeHighCount = taskService.countActiveHighPriorityTasks();
        long total = analytics.getTotalTasks();

        long todoCount       = analytics.getByStatus().getOrDefault(Status.TODO, 0L);
        long inProgressCount = analytics.getByStatus().getOrDefault(Status.IN_PROGRESS, 0L);
        long doneCount       = analytics.getByStatus().getOrDefault(Status.DONE, 0L);
        long lowCount        = analytics.getByPriority().getOrDefault(Priority.LOW, 0L);
        long mediumCount     = analytics.getByPriority().getOrDefault(Priority.MEDIUM, 0L);
        long highCount       = analytics.getByPriority().getOrDefault(Priority.HIGH, 0L);

        long todoPct       = total > 0 ? todoCount       * 100 / total : 0;
        long inProgressPct = total > 0 ? inProgressCount * 100 / total : 0;
        long donePct       = total > 0 ? doneCount       * 100 / total : 0;
        long lowPct        = total > 0 ? lowCount        * 100 / total : 0;
        long mediumPct     = total > 0 ? mediumCount     * 100 / total : 0;
        long highPriPct    = total > 0 ? highCount       * 100 / total : 0;

        long activeHighPct = maxActiveHighPriority > 0
                ? Math.min(100, activeHighCount * 100 / maxActiveHighPriority)
                : 0;

        model.addAttribute("totalTasks",       total);
        model.addAttribute("doneTasks",        doneCount);
        model.addAttribute("inProgressTasks",  inProgressCount);
        model.addAttribute("todoTasks",        todoCount);
        model.addAttribute("overdueTasks",     analytics.getOverdueTasks());
        model.addAttribute("upcomingTasks",    analytics.getUpcomingTasksNext7Days());
        model.addAttribute("activeHighCount",  activeHighCount);
        model.addAttribute("maxActiveHigh",    maxActiveHighPriority);
        model.addAttribute("activeHighPct",    activeHighPct);

        model.addAttribute("todoCount",        todoCount);
        model.addAttribute("todoPct",          todoPct);
        model.addAttribute("inProgressCount",  inProgressCount);
        model.addAttribute("inProgressPct",    inProgressPct);
        model.addAttribute("doneCount",        doneCount);
        model.addAttribute("donePct",          donePct);

        model.addAttribute("lowCount",         lowCount);
        model.addAttribute("lowPct",           lowPct);
        model.addAttribute("mediumCount",      mediumCount);
        model.addAttribute("mediumPct",        mediumPct);
        model.addAttribute("highCount",        highCount);
        model.addAttribute("highPriPct",       highPriPct);

        return "analytics-dashboard";
    }
}
