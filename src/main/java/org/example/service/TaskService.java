package org.example.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.example.Model.dto.TaskForm;
import org.example.Model.entity.*;
import org.example.Model.enums.TaskStatus;
import org.example.exception.*;
import org.example.repository.*;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository tasks;
    private final UserRepository users;
    private final ProjectMemberRepository members;
    private final ProjectService projects;
    private final AccessService access;

    public TaskService(
            TaskRepository tasks,
            UserRepository users,
            ProjectMemberRepository members,
            ProjectService projects,
            AccessService access) {
        this.tasks = tasks;
        this.users = users;
        this.members = members;
        this.projects = projects;
        this.access = access;
    }

    public List<Task> getAllTasks() {
        access.requireAdmin();
        return tasks.findAllByOrderByCreatedAtDesc();
    }

    public List<Task> getTasksForUser(TaskStatus status) {
        User actor = access.currentUser();
        return status == null
                ? tasks.findByAssignedUser(actor)
                : tasks.findByAssignedUserAndStatus(actor, status);
    }

    public Task getTaskById(Long id) {
        Task task = requireTask(id);
        access.requireTaskAccess(task);
        return task;
    }

    public List<Task> getTasksByProject(Long id) {
        Project project = projects.getProjectById(id);
        return access.isAdmin()
                ? tasks.findByProject(project)
                : tasks.findByProjectAndAssignedUser(project, access.currentUser());
    }

    @Transactional
    public Task createTask(Long projectId, TaskForm form) {
        access.requireAdmin();
        Task task = new Task();
        task.setProject(projects.getProjectById(projectId));
        apply(task, form);
        return tasks.saveAndFlush(task);
    }

    @Transactional
    public Task updateTask(Long id, TaskForm form) {
        access.requireAdmin();
        Task task = requireTask(id);
        apply(task, form);
        return tasks.saveAndFlush(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        access.requireAdmin();
        tasks.delete(requireTask(id));
    }

    @Transactional
    public void changeStatus(Long id, TaskStatus status) {
        if (status == null) throw new BusinessException("Wybierz poprawny status.");
        Task task = getTaskById(id);
        task.setStatus(status);
    }

    private Task requireTask(Long id) {
        return tasks.findById(id).orElseThrow(() -> new ResourceNotFoundException("Zadanie"));
    }

    private void apply(Task task, TaskForm form) {
        User assigned = null;
        if (form.getAssignedUserId() != null) {
            assigned =
                    users.findById(form.getAssignedUserId())
                            .filter(User::isEnabled)
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    "assignedUserId",
                                                    "Wybierz aktywnego użytkownika."));
            if (!task.getProject().getOwner().getId().equals(assigned.getId())
                    && !members.existsByProjectAndUser(task.getProject(), assigned))
                throw new BusinessException(
                        "assignedUserId",
                        "Wykonawca musi być właścicielem lub członkiem projektu.");
        }
        task.setTitle(form.getTitle().trim());
        task.setDescription(form.getDescription());
        task.setStatus(form.getStatus());
        task.setPriority(form.getPriority());
        task.setDeadline(form.getDeadline());
        task.setAssignedUser(assigned);
    }
}
