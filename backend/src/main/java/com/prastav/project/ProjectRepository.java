package com.prastav.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Repository pattern: persistence for {@link Project}. Role-scoped list queries live here. */
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByOwner_IdOrderByUpdatedAtDesc(UUID ownerId);

    List<Project> findByReviewer_IdOrderByUpdatedAtDesc(UUID reviewerId);

    List<Project> findAllByOrderByUpdatedAtDesc();
}
