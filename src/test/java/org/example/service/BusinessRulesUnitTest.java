package org.example.service;

import java.util.List;
import java.util.Optional;
import org.example.exception.BusinessException;
import org.example.model.dto.TaskForm;
import org.example.model.dto.UserForm;
import org.example.model.entity.Project;
import org.example.model.entity.ProjectMember;
import org.example.model.entity.Task;
import org.example.model.entity.User;
import org.example.model.enums.Role;
import org.example.model.enums.TaskStatus;
import org.example.repository.ProjectMemberRepository;
import org.example.repository.ProjectRepository;
import org.example.repository.TaskRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessRulesUnitTest {
    @Mock UserRepository users;
    @Mock ProjectRepository projects;
    @Mock ProjectMemberRepository members;
    @Mock TaskRepository tasks;
    @Mock PasswordEncoder encoder;
    @Mock AccessService access;
    @Mock ProjectService projectService;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Odrzuca tworzenie konta z zajętym loginem")
    void rejectsDuplicateUsernameWithoutSavingUser() {
        UserService service = new UserService(users, projects, members, tasks, encoder, access);
        UserForm form = new UserForm();
        form.setUsername("Marek");
        form.setEmail("marek@example.com");
        form.setPassword("Password123!");
        form.setFirstName("Marek");
        form.setLastName("Kowalski");
        when(users.findByUsername("marek")).thenReturn(Optional.of(new User()));

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.createUser(form))
                .withMessage("Ten login jest już zajęty.");

        verify(access).requireAdmin();
        verify(users, never()).saveAndFlush(any(User.class));
        verify(encoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Odrzuca ponowne dodanie członka do projektu")
    void rejectsDuplicateMembershipWithoutSavingMember() {
        ProjectService service = new ProjectService(projects, members, tasks, users, access);
        User owner = mock(User.class);
        User member = mock(User.class);
        Project project = new Project();
        project.setOwner(owner);
        when(owner.getId()).thenReturn(10L);
        when(member.isEnabled()).thenReturn(true);
        when(projects.findById(100L)).thenReturn(Optional.of(project));
        when(users.findById(20L)).thenReturn(Optional.of(member));
        when(members.existsByProjectAndUser(project, member)).thenReturn(true);

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.addMemberToProject(100L, 20L))
                .withMessage("Użytkownik już należy do projektu.");

        verify(access).requireAdmin();
        verify(members, never()).saveAndFlush(any(ProjectMember.class));
    }

    @Test
    @DisplayName("Odrzuca przypisanie zadania osobie spoza projektu")
    void rejectsAssigneeOutsideProjectWithoutSavingTask() {
        TaskService service = new TaskService(tasks, users, members, projectService, access);
        User owner = mock(User.class);
        User outsider = mock(User.class);
        Project project = new Project();
        project.setOwner(owner);
        TaskForm form = new TaskForm();
        form.setTitle("Przygotować materiały");
        form.setAssignedUserId(20L);
        when(owner.getId()).thenReturn(10L);
        when(outsider.getId()).thenReturn(20L);
        when(outsider.isEnabled()).thenReturn(true);
        when(projectService.getProjectById(100L)).thenReturn(project);
        when(users.findById(20L)).thenReturn(Optional.of(outsider));
        when(members.existsByProjectAndUser(project, outsider)).thenReturn(false);

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.createTask(100L, form))
                .withMessage("Wykonawca musi być właścicielem lub członkiem projektu.");

        verify(access).requireAdmin();
        verify(tasks, never()).saveAndFlush(any(Task.class));
    }

    @Test
    @DisplayName("Użytkownik nie może zmienić statusu cudzego zadania")
    void rejectsForeignTaskStatusChangeAndPreservesOriginalStatus() {
        AccessService actualAccess = new AccessService(users, members);
        TaskService service = new TaskService(tasks, users, members, projectService, actualAccess);
        User actor = mock(User.class);
        User assignee = mock(User.class);
        Task task = new Task();
        task.setAssignedUser(assignee);
        task.setStatus(TaskStatus.TODO);
        when(actor.getId()).thenReturn(10L);
        when(actor.getRole()).thenReturn(Role.USER);
        when(actor.isEnabled()).thenReturn(true);
        when(assignee.getId()).thenReturn(20L);
        when(users.findByUsername("anna")).thenReturn(Optional.of(actor));
        when(tasks.findById(300L)).thenReturn(Optional.of(task));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "anna", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> service.changeStatus(300L, TaskStatus.DONE))
                .withMessage("Nie masz dostępu do tego zadania.");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
    }
}
