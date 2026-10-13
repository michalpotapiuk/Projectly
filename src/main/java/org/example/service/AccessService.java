package org.example.service;

import org.example.Model.entity.Project;
import org.example.Model.entity.Task;
import org.example.Model.entity.User;
import org.example.Model.enums.Role;
import org.example.repository.ProjectMemberRepository;
import org.example.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AccessService {
    private final UserRepository users;
    private final ProjectMemberRepository members;

    public AccessService(UserRepository users, ProjectMemberRepository members) {
        this.users = users;
        this.members = members;
    }

    public User currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated())
            throw new AccessDeniedException("Zaloguj się.");
        return users.findByUsername(authentication.getName())
                .filter(User::isEnabled)
                .orElseThrow(
                        () ->
                                new AccessDeniedException(
                                        "Konto jest niedostępne. Zaloguj się ponownie."));
    }

    public boolean isAdmin() {
        return currentUser().getRole() == Role.ADMIN;
    }

    public void requireAdmin() {
        if (!isAdmin()) throw new AccessDeniedException("Ta operacja wymaga roli administratora.");
    }

    public void requireProjectAccess(Project project) {
        User actor = currentUser();
        if (actor.getRole() != Role.ADMIN
                && !project.getOwner().getId().equals(actor.getId())
                && !members.existsByProjectAndUser(project, actor))
            throw new AccessDeniedException("Nie masz dostępu do tego projektu.");
    }

    public void requireTaskAccess(Task task) {
        User actor = currentUser();
        if (actor.getRole() != Role.ADMIN
                && (task.getAssignedUser() == null
                        || !task.getAssignedUser().getId().equals(actor.getId())))
            throw new AccessDeniedException("Nie masz dostępu do tego zadania.");
    }
}
