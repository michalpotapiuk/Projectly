package org.example.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.example.Model.dto.UserForm;
import org.example.Model.entity.*;
import org.example.Model.enums.Role;
import org.example.exception.*;
import org.example.repository.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository users;
    private final ProjectRepository projects;
    private final ProjectMemberRepository members;
    private final TaskRepository tasks;
    private final PasswordEncoder encoder;
    private final org.example.service.AccessService access;

    public UserService(
            UserRepository users,
            ProjectRepository projects,
            ProjectMemberRepository members,
            TaskRepository tasks,
            PasswordEncoder encoder,
            AccessService access) {
        this.users = users;
        this.projects = projects;
        this.members = members;
        this.tasks = tasks;
        this.encoder = encoder;
        this.access = access;
    }

    public List<User> getAllUsers() {
        access.requireAdmin();
        return users.findAll();
    }

    public User getUserById(Long id) {
        access.requireAdmin();
        return requireUser(id);
    }

    public Optional<User> findByUsername(String username) {
        access.requireAdmin();
        return users.findByUsername(username);
    }

    @Transactional
    public User createUser(UserForm form) {
        access.requireAdmin();
        if (form.getPassword() == null
                || form.getPassword().length() < 6
                || form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new BusinessException(
                    "password", "Hasło musi mieć minimum 6 znaków i maksymalnie 72 bajty UTF-8.");
        User user = new User();
        apply(user, form);
        user.setPassword(encoder.encode(form.getPassword()));
        return users.saveAndFlush(user);
    }

    @Transactional
    public User updateUser(Long id, UserForm form) {
        access.requireAdmin();
        User user = requireUser(id);
        protectLastAdmin(user, form.getRole(), form.isEnabled());
        apply(user, form);
        return users.saveAndFlush(user);
    }

    @Transactional
    public void changeRole(Long id, Role role) {
        access.requireAdmin();
        if (role == null) throw new BusinessException("Wybierz rolę.");
        User user = requireUser(id);
        protectLastAdmin(user, role, user.isEnabled());
        user.setRole(role);
    }

    @Transactional
    public void deleteUser(Long id) {
        access.requireAdmin();
        User user = requireUser(id);
        protectLastAdmin(user, user.getRole(), false);
        if (projects.existsByOwner(user)
                || members.existsByUser(user)
                || tasks.existsByAssignedUser(user)) user.setEnabled(false);
        else users.delete(user);
    }

    private User requireUser(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Użytkownik"));
    }

    private void protectLastAdmin(User user, Role role, boolean enabled) {
        if (user.getRole() == Role.ADMIN
                && user.isEnabled()
                && (role != Role.ADMIN || !enabled)
                && users.countByRoleAndEnabledTrue(Role.ADMIN) <= 1)
            throw new BusinessException(
                    "Nie można zablokować ani odebrać roli ostatniemu aktywnemu administratorowi.");
    }

    private void apply(User user, UserForm form) {
        String username = form.getUsername().trim().toLowerCase(Locale.ROOT);
        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
        users.findByUsername(username)
                .filter(
                        existing ->
                                user.getId() == null
                                        || !Objects.equals(existing.getId(), user.getId()))
                .ifPresent(
                        existing -> {
                            throw new BusinessException("username", "Ten login jest już zajęty.");
                        });
        users.findByEmail(email)
                .filter(
                        existing ->
                                user.getId() == null
                                        || !Objects.equals(existing.getId(), user.getId()))
                .ifPresent(
                        existing -> {
                            throw new BusinessException("email", "Ten email jest już zajęty.");
                        });
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(form.getFirstName().trim());
        user.setLastName(form.getLastName().trim());
        user.setRole(form.getRole());
        user.setEnabled(form.isEnabled());
    }
}
