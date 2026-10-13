package org.example;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import org.example.Model.dto.*;
import org.example.Model.entity.*;
import org.example.Model.enums.*;
import org.example.exception.*;
import org.example.repository.*;
import org.example.service.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
class TaskServiceTest {
    @Autowired TaskService service;
    @Autowired ProjectService projectService;
    @Autowired UserRepository users;
    @Autowired ProjectRepository projects;
    @Autowired TaskRepository tasks;

    private Project newProject() {
        var form = new ProjectForm();
        form.setName("Test");
        form.setOwnerId(users.findByUsername("anna").orElseThrow().getId());
        return projectService.createProject(form);
    }

    @Test
    void createsTaskWithDefaultsAndTimestamp() {
        Project project = newProject();
        var form = new TaskForm();
        form.setTitle("Zadanie");
        form.setAssignedUserId(project.getOwner().getId());
        Task task = service.createTask(project.getId(), form);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(task.getCreatedAt()).isNotNull();
        assertThat(task.getProject().getId()).isEqualTo(project.getId());
    }

    @Test
    void rejectsAssigneeOutsideProject() {
        Project project = newProject();
        var form = new TaskForm();
        form.setTitle("Zadanie");
        form.setAssignedUserId(users.findByUsername("jan").orElseThrow().getId());
        assertThatThrownBy(() -> service.createTask(project.getId(), form))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsMissingProject() {
        var form = new TaskForm();
        form.setTitle("Zadanie");
        assertThatThrownBy(() -> service.createTask(99999L, form))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @WithMockUser(username = "anna")
    void retrievesAndFiltersOwnTasks() {
        assertThat(service.getTasksForUser(null))
                .hasSize(2)
                .allMatch(task -> task.getAssignedUser().getUsername().equals("anna"));
        assertThat(service.getTasksForUser(TaskStatus.TODO)).hasSize(1);
    }

    @Test
    @WithMockUser(username = "anna")
    void canChangeOwnStatus() {
        Task task = service.getTasksForUser(TaskStatus.TODO).getFirst();
        service.changeStatus(task.getId(), TaskStatus.DONE);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    @WithMockUser(username = "anna")
    void cannotReadOrChangeAnotherUsersTask() {
        Task foreign =
                tasks.findByAssignedUser(users.findByUsername("jan").orElseThrow()).getFirst();
        assertThatThrownBy(() -> service.getTaskById(foreign.getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.changeStatus(foreign.getId(), TaskStatus.DONE))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "anna")
    void cannotEditTaskEvenWhenAssigned() {
        Task task = service.getTasksForUser(null).getFirst();
        var form = new TaskForm();
        form.setTitle("Zmiana");
        assertThatThrownBy(() -> service.updateTask(task.getId(), form))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(username = "anna")
    void blockedAccountCannotContinueUsingExistingSession() {
        users.findByUsername("anna").orElseThrow().setEnabled(false);
        assertThatThrownBy(() -> service.getTasksForUser(null))
                .isInstanceOf(AccessDeniedException.class);
    }
}
