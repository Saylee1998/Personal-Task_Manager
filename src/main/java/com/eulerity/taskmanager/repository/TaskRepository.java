package com.eulerity.taskmanager.repository;

import com.eulerity.taskmanager.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link Task} persistence operations.
 * Inherits full CRUD and pagination support from {@link JpaRepository}.
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
}