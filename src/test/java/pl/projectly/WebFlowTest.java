package org.example;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.example.Model.dto.ProjectForm;
import org.example.repository.*;
import org.example.service.ProjectService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext(
        classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class WebFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ProjectRepository projects;
    @Autowired TaskRepository tasks;
    @Autowired UserRepository users;
    @Autowired ProjectService projectService;

    @Test
    void loginIsPublicAndProjectsRequireLogin() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Projectly")));
        mvc.perform(get("/projects")).andExpect(status().is3xxRedirection());
    }

    @Test
    void realDatabaseLoginWorks() throws Exception {
        mvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("username", "admin")
                                .param("password", "TestPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void wrongPasswordReturnsLoginError() throws Exception {
        mvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("username", "admin")
                                .param("password", "incorrect"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void rendersEveryAdminViewAndIncludesCsrf() throws Exception {
        Long projectId = projects.findAll().getFirst().getId();
        Long taskId = tasks.findAll().getFirst().getId();
        Long userId = users.findByUsername("anna").orElseThrow().getId();
        String[] paths = {
            "/dashboard",
            "/projects",
            "/projects/new",
            "/projects/" + projectId,
            "/projects/" + projectId + "/edit",
            "/tasks",
            "/my-tasks",
            "/tasks/" + taskId,
            "/tasks/" + taskId + "/edit",
            "/projects/" + projectId + "/tasks/new",
            "/users",
            "/users/new",
            "/users/" + userId + "/edit"
        };
        for (String path : paths)
            mvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Projectly")));
        mvc.perform(get("/projects/new"))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    @WithMockUser(username = "anna")
    void rendersUserViewsAndDeniesAdminViews() throws Exception {
        Long ownTaskId =
                tasks.findByAssignedUser(users.findByUsername("anna").orElseThrow())
                        .getFirst()
                        .getId();
        for (String path :
                new String[] {
                    "/dashboard", "/projects", "/my-tasks", "/tasks", "/tasks/" + ownTaskId
                }) mvc.perform(get(path)).andExpect(status().isOk());
        mvc.perform(get("/users")).andExpect(status().isForbidden());
        mvc.perform(get("/projects/new")).andExpect(status().isForbidden());
        mvc.perform(get("/tasks/" + ownTaskId + "/edit")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void validationErrorsAppearUnderFields() throws Exception {
        mvc.perform(post("/projects").with(csrf()).param("name", "").param("ownerId", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "name", "ownerId"))
                .andExpect(content().string(containsString("Pole jest wymagane")));
        mvc.perform(
                        post("/users")
                                .with(csrf())
                                .param("username", "student")
                                .param("email", "wrong")
                                .param("firstName", "Test")
                                .param("lastName", "Test")
                                .param("password", "x")
                                .param("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "email", "password"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void duplicateUserReturnsFormError() throws Exception {
        mvc.perform(
                        post("/users")
                                .with(csrf())
                                .param("username", "admin")
                                .param("email", "new@example.org")
                                .param("password", "Password123!")
                                .param("firstName", "Test")
                                .param("lastName", "Test")
                                .param("role", "USER")
                                .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "username"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void postWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/projects").param("name", "Test").param("ownerId", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "anna")
    void forgedStatusChangeIsForbidden() throws Exception {
        Long foreignId =
                tasks.findByAssignedUser(users.findByUsername("jan").orElseThrow())
                        .getFirst()
                        .getId();
        mvc.perform(post("/tasks/" + foreignId + "/status").with(csrf()).param("status", "DONE"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void missingResourceAndInvalidStatusHaveFriendlyErrors() throws Exception {
        mvc.perform(get("/tasks/999999")).andExpect(status().isNotFound());
        Long taskId = tasks.findAll().getFirst().getId();
        mvc.perform(post("/tasks/" + taskId + "/status").with(csrf()).param("status", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void projectCreationRedirectsToDetails() throws Exception {
        mvc.perform(
                        post("/projects")
                                .with(csrf())
                                .param("name", "Projekt MVC")
                                .param("description", "Opis")
                                .param(
                                        "ownerId",
                                        users.findByUsername("anna")
                                                .orElseThrow()
                                                .getId()
                                                .toString()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void userCannotOpenProjectWithoutMembership() throws Exception {
        var form = new ProjectForm();
        form.setName("Prywatny projekt");
        form.setOwnerId(users.findByUsername("admin").orElseThrow().getId());
        Long projectId = projectService.createProject(form).getId();
        mvc.perform(
                        get("/projects/" + projectId)
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors.user("anna")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void fullAdminCrudWorkflowMaintainsRelationships() throws Exception {
        mvc.perform(
                        post("/users")
                                .with(csrf())
                                .param("username", "mvcstudent")
                                .param("email", "mvcstudent@example.org")
                                .param("password", "Password123!")
                                .param("firstName", "Student")
                                .param("lastName", "MVC")
                                .param("role", "USER")
                                .param("enabled", "true"))
                .andExpect(redirectedUrl("/users"));
        Long userId = users.findByUsername("mvcstudent").orElseThrow().getId();
        String location =
                mvc.perform(
                                post("/projects")
                                        .with(csrf())
                                        .param("name", "Projekt CRUD")
                                        .param(
                                                "ownerId",
                                                users.findByUsername("admin")
                                                        .orElseThrow()
                                                        .getId()
                                                        .toString()))
                        .andExpect(status().is3xxRedirection())
                        .andReturn()
                        .getResponse()
                        .getRedirectedUrl();
        Long projectId = Long.valueOf(location.substring(location.lastIndexOf('/') + 1));
        mvc.perform(post(location + "/members").with(csrf()).param("userId", userId.toString()))
                .andExpect(redirectedUrl(location));
        String taskLocation =
                mvc.perform(
                                post(location + "/tasks")
                                        .with(csrf())
                                        .param("title", "Zadanie CRUD")
                                        .param("assignedUserId", userId.toString())
                                        .param("status", "TODO")
                                        .param("priority", "HIGH")
                                        .param("deadline", "2027-01-15"))
                        .andExpect(status().is3xxRedirection())
                        .andReturn()
                        .getResponse()
                        .getRedirectedUrl();
        Long taskId = Long.valueOf(taskLocation.substring(taskLocation.lastIndexOf('/') + 1));
        mvc.perform(
                        post(taskLocation + "/edit")
                                .with(csrf())
                                .param("title", "Edytowane zadanie")
                                .param("assignedUserId", userId.toString())
                                .param("status", "IN_PROGRESS")
                                .param("priority", "MEDIUM"))
                .andExpect(redirectedUrl(taskLocation));
        mvc.perform(
                        post(taskLocation + "/status")
                                .with(csrf())
                                .param("status", "DONE")
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors.user(
                                                "mvcstudent")))
                .andExpect(redirectedUrl(taskLocation));
        mvc.perform(post(location + "/members/" + userId + "/delete").with(csrf()))
                .andExpect(redirectedUrl(location));
        org.assertj.core.api.Assertions.assertThat(
                        tasks.findById(taskId).orElseThrow().getAssignedUser())
                .isNull();
        mvc.perform(
                        post(location + "/edit")
                                .with(csrf())
                                .param("name", "Edytowany projekt")
                                .param(
                                        "ownerId",
                                        users.findByUsername("admin")
                                                .orElseThrow()
                                                .getId()
                                                .toString()))
                .andExpect(redirectedUrl(location));
        mvc.perform(post(location + "/delete").with(csrf())).andExpect(redirectedUrl("/projects"));
        org.assertj.core.api.Assertions.assertThat(projects.findById(projectId)).isEmpty();
        org.assertj.core.api.Assertions.assertThat(tasks.findById(taskId)).isEmpty();
        mvc.perform(post("/users/" + userId + "/delete").with(csrf()))
                .andExpect(redirectedUrl("/users"));
        org.assertj.core.api.Assertions.assertThat(users.findById(userId)).isEmpty();
    }
}
