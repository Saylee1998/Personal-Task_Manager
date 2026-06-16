package com.project.taskmanager.controller;

import com.project.taskmanager.model.Status;
import com.project.taskmanager.dto.TaskAnalyticsResponse;
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
}
