package org.example.repository;

import org.springframework.data.jpa.repository.*;

import org.example.Model.entity.*;
import org.example.Model.enums.TaskStatus;

import java.util.*;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @EntityGraph(attributePaths = {"project", "project.owner", "assignedUser"})
    List<Task> findAllByOrderByCreatedAtDesc();

    @Override
    @EntityGraph(attributePaths = {"project", "project.owner", "assignedUser"})
    Optional<Task> findById(Long id);

    @EntityGraph(attributePaths = {"project", "assignedUser"})
    List<Task> findByProject(Project project);

    @EntityGraph(attributePaths = {"project", "assignedUser"})
    List<Task> findByAssignedUser(User user);

    @EntityGraph(attributePaths = {"project", "assignedUser"})
    List<Task> findByStatus(TaskStatus status);

    @EntityGraph(attributePaths = {"project", "assignedUser"})
    List<Task> findByAssignedUserAndStatus(User user, TaskStatus status);

    @EntityGraph(attributePaths = {"project", "assignedUser"})
    List<Task> findByProjectAndAssignedUser(Project project, User user);

    boolean existsByAssignedUser(User user);

    long countByProject(Project project);
}
