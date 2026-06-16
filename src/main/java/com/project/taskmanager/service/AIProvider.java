package com.project.taskmanager.service;

import com.project.taskmanager.dto.AITaskSuggestionResponse;
import com.project.taskmanager.dto.TaskBreakdownResponse;
import com.project.taskmanager.dto.TaskResponse;

public interface AIProvider {

    AITaskSuggestionResponse suggestTask(String description);

    TaskBreakdownResponse breakdownTask(TaskResponse task);
}
