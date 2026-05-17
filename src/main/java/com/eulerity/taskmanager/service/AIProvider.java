package com.eulerity.taskmanager.service;

import com.eulerity.taskmanager.dto.AITaskSuggestionResponse;
import com.eulerity.taskmanager.dto.TaskBreakdownResponse;
import com.eulerity.taskmanager.dto.TaskResponse;

public interface AIProvider {

    AITaskSuggestionResponse suggestTask(String description);

    TaskBreakdownResponse breakdownTask(TaskResponse task);
}
