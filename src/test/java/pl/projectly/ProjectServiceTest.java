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
import org.example.exception.*;
import org.example.repository.*;
import org.example.service.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
class ProjectServiceTest {
    @Autowired ProjectService service;
    @Autowired UserRepository users;
    @Autowired ProjectMemberRepository members;
    @Autowired TaskRepository tasks;
    @Autowired TaskService taskService;
    @Autowired jakarta.persistence.EntityManager entityManager;

    private Project createProject() {
        var form = new ProjectForm();
        form.setName("Projekt testowy");
        form.setDescription("Opis");
        form.setOwnerId(users.findByUsername("anna").orElseThrow().getId());
        return service.createProject(form);
    }

    @Test
    void createsProjectWithAutomaticTimestampAndOwnerAccess() {
        Project project = createProject();
        assertThat(project.getCreatedAt()).isNotNull();
        assertThat(project.getOwner().getUsername()).isEqualTo("anna");
        assertThat(members.existsByProjectAndUser(project, project.getOwner())).isFalse();
    }

    @Test
    void addsMember() {
        Project project = createProject();
        User jan = users.findByUsername("jan").orElseThrow();
        service.addMemberToProject(project.getId(), jan.getId());
        assertThat(members.existsByProjectAndUser(project, jan)).isTrue();
        assertThat(service.getProjectMembers(project.getId()).getFirst().getJoinedAt()).isNotNull();
    }

    @Test
    void rejectsDuplicateMembership() {
        Project project = createProject();
        User jan = users.findByUsername("jan").orElseThrow();
        service.addMemberToProject(project.getId(), jan.getId());
        assertThatThrownBy(() -> service.addMemberToProject(project.getId(), jan.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsAddingOwnerAsMember() {
        Project project = createProject();
        assertThatThrownBy(
                        () ->
                                service.addMemberToProject(
                                        project.getId(), project.getOwner().getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void removingMemberUnassignsTasks() {
        Project project = createProject();
        User jan = users.findByUsername("jan").orElseThrow();
        service.addMemberToProject(project.getId(), jan.getId());
        var form = new TaskForm();
        form.setTitle("Zadanie");
        form.setAssignedUserId(jan.getId());
        Task task = taskService.createTask(project.getId(), form);
        service.removeMemberFromProject(project.getId(), jan.getId());
        assertThat(task.getAssignedUser()).isNull();
        assertThat(members.existsByProjectAndUser(project, jan)).isFalse();
    }

    @Test
    void changingOwnerPreservesFormerOwnerMembership() {
        Project project = createProject();
        User formerOwner = project.getOwner();
        User jan = users.findByUsername("jan").orElseThrow();
        service.addMemberToProject(project.getId(), jan.getId());
        var form = new ProjectForm();
        form.setName("Nowa nazwa");
        form.setOwnerId(jan.getId());
        service.updateProject(project.getId(), form);
        assertThat(members.existsByProjectAndUser(project, formerOwner)).isTrue();
        assertThat(members.existsByProjectAndUser(project, jan)).isFalse();
    }

    @Test
    void deletionCascadesOnlyToTasksAndMemberships() {
        Project project = createProject();
        User jan = users.findByUsername("jan").orElseThrow();
        service.addMemberToProject(project.getId(), jan.getId());
        var form = new TaskForm();
        form.setTitle("Zadanie");
        Task task = taskService.createTask(project.getId(), form);
        Long taskId = task.getId();
        Long projectId = project.getId();
        entityManager.flush();
        entityManager.clear();
        service.deleteProject(projectId);
        entityManager.flush();
        entityManager.clear();
        assertThat(tasks.findById(taskId)).isEmpty();
        assertThat(users.findById(jan.getId())).isPresent();
        assertThat(members.findAll())
                .noneMatch(member -> member.getProject().getId().equals(projectId));
    }

    @Test
    @WithMockUser(username = "anna")
    void regularUserSeesOnlyAccessibleProjects() {
        assertThat(service.getProjectsForUser()).hasSize(2);
        assertThatThrownBy(this::createProject).isInstanceOf(AccessDeniedException.class);
    }
}
