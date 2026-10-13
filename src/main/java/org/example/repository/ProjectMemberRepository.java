package org.example.repository;

import org.springframework.data.jpa.repository.*;

import org.example.model.entity.*;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    @EntityGraph(attributePaths = {"user", "project", "project.owner"})
    List<ProjectMember> findByUser(User user);

    @EntityGraph(attributePaths = "user")
    List<ProjectMember> findByProject(Project project);

    boolean existsByProjectAndUser(Project project, User user);

    void deleteByProjectAndUser(Project project, User user);

    boolean existsByUser(User user);
}
