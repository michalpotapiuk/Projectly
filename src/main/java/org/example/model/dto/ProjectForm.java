package org.example.model.dto;

import jakarta.validation.constraints.*;

public class ProjectForm {

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull(message = "Wybierz właściciela")
    private Long ownerId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }
}
