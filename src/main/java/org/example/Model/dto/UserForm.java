package org.example.Model.dto;

import jakarta.validation.constraints.*;

import org.example.Model.enums.Role;

public class UserForm {
    public interface Create extends jakarta.validation.groups.Default {}

    @NotBlank
    @Pattern(regexp = "[a-zA-Z0-9._-]{3,50}", message = "Login: 3–50 liter, cyfr lub znaków ._-")
    private String username;

    @NotBlank
    @Email
    @Size(max = 120)
    private String email;

    @NotBlank(groups = Create.class)
    @Size(min = 6, max = 72, groups = Create.class)
    private String password;

    @NotBlank
    @Size(max = 80)
    private String firstName;

    @NotBlank
    @Size(max = 80)
    private String lastName;

    @NotNull private Role role = Role.USER;

    private boolean enabled = true;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
