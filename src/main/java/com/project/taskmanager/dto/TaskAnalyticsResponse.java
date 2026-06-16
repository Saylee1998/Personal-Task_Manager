package com.project.taskmanager.dto;

import com.project.taskmanager.model.Priority;
import com.project.taskmanager.model.Status;
import java.util.Collections;
import java.util.Map;

public class TaskAnalyticsResponse {

    private final long totalTasks;
    private final Map<Status, Long> byStatus;
    private final Map<Priority, Long> byPriority;
    private final long overdueTasks;
    private final long upcomingTasksNext7Days;

    public TaskAnalyticsResponse(long totalTasks, Map<Status, Long> byStatus,
                                  Map<Priority, Long> byPriority,
                                  long overdueTasks, long upcomingTasksNext7Days) {
        this.totalTasks             = totalTasks;
        this.byStatus               = Collections.unmodifiableMap(byStatus);
        this.byPriority             = Collections.unmodifiableMap(byPriority);
        this.overdueTasks           = overdueTasks;
        this.upcomingTasksNext7Days = upcomingTasksNext7Days;
    }

    public long getTotalTasks()                { return totalTasks; }
    public Map<Status, Long> getByStatus()     { return byStatus; }
    public Map<Priority, Long> getByPriority() { return byPriority; }
    public long getOverdueTasks()              { return overdueTasks; }
    public long getUpcomingTasksNext7Days()    { return upcomingTasksNext7Days; }
}
