package com.prastav.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

/**
 * Repository pattern: persistence for {@link Project}. The list is queried through
 * {@link ProjectSpecifications}; the grouped counts feed the dashboard summary.
 */
public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {

    @Query("select p.status as status, count(p) as count from Project p where p.owner.id = :ownerId group by p.status")
    List<ProjectStatusCount> countByStatusForOwner(UUID ownerId);

    @Query("select p.status as status, count(p) as count from Project p where p.reviewer.id = :reviewerId group by p.status")
    List<ProjectStatusCount> countByStatusForReviewer(UUID reviewerId);

    @Query("select p.status as status, count(p) as count from Project p group by p.status")
    List<ProjectStatusCount> countByStatus();
}
