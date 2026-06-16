package com.project.taskmanager.repository;

import com.project.taskmanager.model.Priority;
import com.project.taskmanager.model.Status;
import com.project.taskmanager.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link Task} persistence operations.
 * Inherits full CRUD and pagination support from {@link JpaRepository}.
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    long countByPriorityAndStatusNot(Priority priority, Status status);

    long countByPriorityAndStatusNotAndIdNot(Priority priority, Status status, Long id);
}