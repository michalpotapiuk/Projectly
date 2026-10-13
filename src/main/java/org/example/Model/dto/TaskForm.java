package org.example.Model.dto;

import jakarta.validation.constraints.*;

import org.example.Model.enums.Priority;
import org.example.Model.enums.TaskStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class TaskForm {

    @NotBlank
    @Size(max = 160)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotNull private TaskStatus status = TaskStatus.TODO;
    @NotNull private Priority priority = Priority.MEDIUM;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;

    private Long assignedUserId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public Long getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(Long assignedUserId) {
        this.assignedUserId = assignedUserId;
    }
}
