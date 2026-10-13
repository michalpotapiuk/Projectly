package org.example.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.example.Model.dto.ProjectForm;
import org.example.Model.entity.*;
import org.example.exception.*;
import org.example.repository.*;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class ProjectService {
    private final ProjectRepository projects;
    private final ProjectMemberRepository members;
    private final TaskRepository tasks;
    private final UserRepository users;
    private final org.example.service.AccessService access;

    public ProjectService(
            ProjectRepository projects,
            ProjectMemberRepository members,
            TaskRepository tasks,
            UserRepository users,
            AccessService access) {
        this.projects = projects;
        this.members = members;
        this.tasks = tasks;
        this.users = users;
        this.access = access;
    }

    public List<Project> getAllProjects() {
        access.requireAdmin();
        return projects.findAllByOrderByCreatedAtDesc();
    }

    public List<Project> getProjectsForUser() {
        return access.isAdmin()
                ? projects.findAllByOrderByCreatedAtDesc()
                : projects.findAccessible(access.currentUser());
    }

    public Project getProjectById(Long id) {
        Project project = requireProject(id);
        access.requireProjectAccess(project);
        return project;
    }

    public long getTaskCount(Project project) {
        access.requireProjectAccess(project);
        return tasks.countByProject(project);
    }

    public List<ProjectMember> getProjectMembers(Long id) {
        return members.findByProject(getProjectById(id));
    }

    public List<User> getAssignableUsers(Long id) {
        Project project = getProjectById(id);
        var result = new LinkedHashMap<Long, User>();
        if (project.getOwner().isEnabled())
            result.put(project.getOwner().getId(), project.getOwner());
        for (ProjectMember member : members.findByProject(project))
            if (member.getUser().isEnabled())
                result.put(member.getUser().getId(), member.getUser());
        return List.copyOf(result.values());
    }

    @Transactional
    public Project createProject(ProjectForm form) {
        access.requireAdmin();
        Project project = new Project();
        apply(project, form);
        return projects.saveAndFlush(project);
    }

    @Transactional
    public Project updateProject(Long id, ProjectForm form) {
        access.requireAdmin();
        Project project = requireProject(id);
        User previousOwner = project.getOwner();
        User nextOwner = requireActiveUser(form.getOwnerId());
        if (!previousOwner.getId().equals(nextOwner.getId())) {
            if (!members.existsByProjectAndUser(project, previousOwner)) {
                ProjectMember member = new ProjectMember();
                member.setProject(project);
                member.setUser(previousOwner);
                members.save(member);
            }
            members.deleteByProjectAndUser(project, nextOwner);
        }
        apply(project, form);
        return projects.saveAndFlush(project);
    }

    @Transactional
    public void deleteProject(Long id) {
        access.requireAdmin();
        projects.delete(requireProject(id));
    }

    @Transactional
    public void addMemberToProject(Long projectId, Long userId) {
        access.requireAdmin();
        Project project = requireProject(projectId);
        User user = requireActiveUser(userId);
        if (project.getOwner().getId().equals(userId))
            throw new BusinessException("Właściciel już ma dostęp do projektu.");
        if (members.existsByProjectAndUser(project, user))
            throw new BusinessException("Użytkownik już należy do projektu.");
        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        members.saveAndFlush(member);
    }

    @Transactional
    public void removeMemberFromProject(Long projectId, Long userId) {
        access.requireAdmin();
        Project project = requireProject(projectId);
        User user =
                users.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Użytkownik"));
        if (project.getOwner().getId().equals(userId))
            throw new BusinessException("Nie można usunąć właściciela.");
        if (!members.existsByProjectAndUser(project, user))
            throw new BusinessException("Użytkownik nie jest członkiem projektu.");
        for (Task task : tasks.findByProjectAndAssignedUser(project, user))
            task.setAssignedUser(null);
        members.deleteByProjectAndUser(project, user);
    }

    private Project requireProject(Long id) {
        return projects.findById(id).orElseThrow(() -> new ResourceNotFoundException("Projekt"));
    }

    private User requireActiveUser(Long id) {
        return users.findById(id)
                .filter(User::isEnabled)
                .orElseThrow(() -> new BusinessException("Wybierz aktywnego użytkownika."));
    }

    private void apply(Project project, ProjectForm form) {
        project.setName(form.getName().trim());
        project.setDescription(form.getDescription());
        project.setOwner(requireActiveUser(form.getOwnerId()));
    }
}
