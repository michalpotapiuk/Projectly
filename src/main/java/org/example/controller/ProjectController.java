package org.example.controller;

import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.example.model.dto.ProjectForm;
import org.example.exception.BusinessException;
import org.example.service.*;

import java.util.*;

@Controller
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectService projects;
    private final UserService users;
    private final TaskService tasks;
    private final AccessService access;

    public ProjectController(
            ProjectService projects, UserService users, TaskService tasks, AccessService access) {
        this.projects = projects;
        this.users = users;
        this.tasks = tasks;
        this.access = access;
    }

    @GetMapping
    public String list(Model model) {
        var visible = projects.getProjectsForUser();
        var counts = new HashMap<Long, Long>();
        visible.forEach(project -> counts.put(project.getId(), projects.getTaskCount(project)));
        model.addAttribute("projects", visible);
        model.addAttribute("taskCounts", counts);
        return "projects/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("project", projects.getProjectById(id));
        model.addAttribute("members", projects.getProjectMembers(id));
        model.addAttribute("tasks", tasks.getTasksByProject(id));
        if (access.isAdmin())
            model.addAttribute(
                    "users",
                    users.getAllUsers().stream().filter(user -> user.isEnabled()).toList());
        return "projects/details";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        access.requireAdmin();
        model.addAttribute("form", new ProjectForm());
        return form(model, null);
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("form") ProjectForm form, BindingResult errors, Model model) {
        access.requireAdmin();
        if (errors.hasErrors()) return form(model, null);
        try {
            return "redirect:/projects/" + projects.createProject(form).getId();
        } catch (BusinessException exception) {
            FormErrors.reject(errors, exception);
            return form(model, null);
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        access.requireAdmin();
        var project = projects.getProjectById(id);
        var form = new ProjectForm();
        form.setName(project.getName());
        form.setDescription(project.getDescription());
        form.setOwnerId(project.getOwner().getId());
        model.addAttribute("form", form);
        return form(model, id);
    }

    @PostMapping("/{id}/edit")
    public String edit(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") ProjectForm form,
            BindingResult errors,
            Model model) {
        access.requireAdmin();
        if (errors.hasErrors()) return form(model, id);
        try {
            projects.updateProject(id, form);
            return "redirect:/projects/" + id;
        } catch (BusinessException exception) {
            FormErrors.reject(errors, exception);
            return form(model, id);
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        projects.deleteProject(id);
        return "redirect:/projects";
    }

    @PostMapping("/{id}/members")
    public String addMember(
            @PathVariable Long id, @RequestParam Long userId, RedirectAttributes redirect) {
        try {
            projects.addMemberToProject(id, userId);
            redirect.addFlashAttribute("success", "Dodano członka projektu.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        } catch (DataIntegrityViolationException exception) {
            redirect.addFlashAttribute("error", "Użytkownik już należy do projektu.");
        }
        return "redirect:/projects/" + id;
    }

    @PostMapping("/{projectId}/members/{userId}/delete")
    public String removeMember(
            @PathVariable Long projectId, @PathVariable Long userId, RedirectAttributes redirect) {
        try {
            projects.removeMemberFromProject(projectId, userId);
            redirect.addFlashAttribute(
                    "success", "Usunięto członka; jego zadania oczekują na przypisanie.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/projects/" + projectId;
    }

    private String form(Model model, Long id) {
        model.addAttribute("projectId", id);
        model.addAttribute(
                "users", users.getAllUsers().stream().filter(user -> user.isEnabled()).toList());
        return "projects/form";
    }
}
