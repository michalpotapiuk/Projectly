package org.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.example.model.enums.TaskStatus;
import org.example.service.*;

@Controller
public class DashboardController {
    private final AccessService access;
    private final ProjectService projects;
    private final TaskService tasks;

    public DashboardController(AccessService access, ProjectService projects, TaskService tasks) {
        this.access = access;
        this.projects = projects;
        this.tasks = tasks;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        var visibleTasks = access.isAdmin() ? tasks.getAllTasks() : tasks.getTasksForUser(null);
        model.addAttribute("projectCount", projects.getProjectsForUser().size());
        model.addAttribute("taskCount", visibleTasks.size());
        for (TaskStatus status : TaskStatus.values())
            model.addAttribute(
                    status.name(),
                    visibleTasks.stream().filter(task -> task.getStatus() == status).count());
        return "dashboard";
    }
}
