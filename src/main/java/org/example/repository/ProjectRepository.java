package org.example.repository;

import org.example.model.entity.User;
import org.springframework.data.jpa.repository.*;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<org.example.model.entity.Project, Long> {
    @EntityGraph(attributePaths = "owner")
    List<org.example.model.entity.Project> findAllByOrderByCreatedAtDesc();

    @Override
    @EntityGraph(attributePaths = "owner")
    Optional<org.example.model.entity.Project> findById(Long id);

    @EntityGraph(attributePaths = "owner")
    List<org.example.model.entity.Project> findByOwner(org.example.model.entity.User owner);

    @EntityGraph(attributePaths = "owner")
    @Query(
            "select distinct project from Project project where project.owner = :user or exists"
                + " (select member.id from ProjectMember member where member.project = project and"
                + " member.user = :user) order by project.createdAt desc")
    List<org.example.model.entity.Project> findAccessible(org.example.model.entity.User user);

    boolean existsByOwner(User owner);
}
