package org.example.Controller;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import org.example.Model.dto.TaskForm;
import org.example.Model.entity.Task;
import org.example.Model.enums.*;
import org.example.exception.BusinessException;
import org.example.service.*;

@Controller
public class TaskController {
    private final TaskService tasks;
    private final ProjectService projects;
    private final AccessService access;

    public TaskController(TaskService tasks, ProjectService projects, AccessService access) {
        this.tasks = tasks;
        this.projects = projects;
        this.access = access;
    }

    @GetMapping("/tasks")
    public String list(Model model) {
        model.addAttribute(
                "tasks", access.isAdmin() ? tasks.getAllTasks() : tasks.getTasksForUser(null));
        model.addAttribute("pageTitle", access.isAdmin() ? "Wszystkie zadania" : "Moje zadania");
        return "tasks/list";
    }

    @GetMapping("/my-tasks")
    public String mine(@RequestParam(required = false) TaskStatus status, Model model) {
        model.addAttribute("tasks", tasks.getTasksForUser(status));
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("pageTitle", "Moje zadania");
        return "tasks/my-tasks";
    }

    @GetMapping("/tasks/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("task", tasks.getTaskById(id));
        model.addAttribute("statuses", TaskStatus.values());
        return "tasks/details";
    }

    @GetMapping("/projects/{projectId}/tasks/new")
    public String createForm(@PathVariable Long projectId, Model model) {
        access.requireAdmin();
        model.addAttribute("form", new TaskForm());
        return form(model, projectId, null);
    }

    @PostMapping("/projects/{projectId}/tasks")
    public String create(
            @PathVariable Long projectId,
            @Valid @ModelAttribute("form") TaskForm form,
            BindingResult errors,
            Model model) {
        access.requireAdmin();
        if (errors.hasErrors()) return form(model, projectId, null);
        try {
            return "redirect:/tasks/" + tasks.createTask(projectId, form).getId();
        } catch (BusinessException exception) {
            FormErrors.reject(errors, exception);
            return form(model, projectId, null);
        }
    }

    @GetMapping("/tasks/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        access.requireAdmin();
        Task task = tasks.getTaskById(id);
        var form = new TaskForm();
        form.setTitle(task.getTitle());
        form.setDescription(task.getDescription());
        form.setStatus(task.getStatus());
        form.setPriority(task.getPriority());
        form.setDeadline(task.getDeadline());
        form.setAssignedUserId(
                task.getAssignedUser() == null ? null : task.getAssignedUser().getId());
        model.addAttribute("form", form);
        return form(model, task.getProject().getId(), id);
    }

    @PostMapping("/tasks/{id}/edit")
    public String edit(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") TaskForm form,
            BindingResult errors,
            Model model) {
        access.requireAdmin();
        Task task = tasks.getTaskById(id);
        if (errors.hasErrors()) return form(model, task.getProject().getId(), id);
        try {
            tasks.updateTask(id, form);
            return "redirect:/tasks/" + id;
        } catch (BusinessException exception) {
            FormErrors.reject(errors, exception);
            return form(model, task.getProject().getId(), id);
        }
    }

    @PostMapping("/tasks/{id}/delete")
    public String delete(@PathVariable Long id) {
        access.requireAdmin();
        Long projectId = tasks.getTaskById(id).getProject().getId();
        tasks.deleteTask(id);
        return "redirect:/projects/" + projectId;
    }

    @PostMapping("/tasks/{id}/status")
    public String status(@PathVariable Long id, @RequestParam TaskStatus status) {
        tasks.changeStatus(id, status);
        return "redirect:/tasks/" + id;
    }

    private String form(Model model, Long projectId, Long taskId) {
        model.addAttribute("project", projects.getProjectById(projectId));
        model.addAttribute("taskId", taskId);
        model.addAttribute("users", projects.getAssignableUsers(projectId));
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("priorities", Priority.values());
        return "tasks/form";
    }
}
