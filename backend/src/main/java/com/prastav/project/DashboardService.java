package com.prastav.project;

import com.prastav.auth.Actor;
import com.prastav.auth.Role;
import com.prastav.workflow.ProjectStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the coordinator dashboard from grouped database queries, so no project list is loaded to count it.
 * An open review is a project UNDER_REVIEW: the reviewer still owes a decision.
 */
@Service
public class DashboardService {

    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final ProjectAccessPolicy accessPolicy;

    public DashboardService(ProjectService projectService, ProjectRepository projectRepository,
                            ProjectAccessPolicy accessPolicy) {
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary(Actor actor) {
        accessPolicy.requireRole(actor, Role.COORDINATOR);
        ProjectCounts counts = projectService.countProjects(actor);
        return new DashboardSummary(counts.total(), counts.byStatus(),
                projectRepository.findAwaitingAssignment(ProjectStatus.SUBMITTED),
                projectRepository.reviewerWorkload(Role.FACULTY, ProjectStatus.UNDER_REVIEW));
    }
}
