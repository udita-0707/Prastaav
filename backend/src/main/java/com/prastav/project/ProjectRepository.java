package com.prastav.project;

import com.prastav.auth.Role;
import com.prastav.workflow.ProjectStatus;
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

    @Query("select p.id as id, p.title as title, o.id as ownerId, o.name as ownerName, p.updatedAt as waitingSince"
            + " from Project p join p.owner o where p.status = :status and p.reviewer is null"
            + " order by p.updatedAt asc")
    List<ProjectAwaitingAssignment> findAwaitingAssignment(ProjectStatus status);

    /** Every user with {@code role}, with the number of their projects in {@code status} (zero included). */
    @Query("select u.id as reviewerId, u.name as reviewerName, count(p) as openReviews"
            + " from User u left join Project p on p.reviewer = u and p.status = :status"
            + " where u.role = :role group by u.id, u.name order by count(p) desc, u.name asc")
    List<ReviewerWorkload> reviewerWorkload(Role role, ProjectStatus status);
}
