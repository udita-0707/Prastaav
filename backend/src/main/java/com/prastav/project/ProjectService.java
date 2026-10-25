package com.prastav.project;

import com.prastav.auth.Actor;
import com.prastav.auth.Role;
import com.prastav.auth.User;
import com.prastav.auth.UserService;
import com.prastav.proposal.ProposalContent;
import com.prastav.proposal.ProposalService;
import com.prastav.proposal.ProposalVersion;
import com.prastav.workflow.AllowedActionsResolver;
import com.prastav.workflow.ProjectStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Use cases for creating, listing and viewing projects. Status changes go through WorkflowService. */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectStore projectStore;
    private final ProjectAccessPolicy accessPolicy;
    private final ProposalService proposalService;
    private final AllowedActionsResolver allowedActionsResolver;
    private final UserService userService;
    private final ApplicationEventPublisher events;

    public ProjectService(ProjectRepository projectRepository, ProjectStore projectStore,
                          ProjectAccessPolicy accessPolicy, ProposalService proposalService,
                          AllowedActionsResolver allowedActionsResolver, UserService userService,
                          ApplicationEventPublisher events) {
        this.projectRepository = projectRepository;
        this.projectStore = projectStore;
        this.accessPolicy = accessPolicy;
        this.proposalService = proposalService;
        this.allowedActionsResolver = allowedActionsResolver;
        this.userService = userService;
        this.events = events;
    }

    /** Creates the project in DRAFT together with proposal version 1 (also a draft). */
    @Transactional
    public ProjectView createProject(NewProject newProject, ProposalContent proposal, Actor actor) {
        accessPolicy.requireRole(actor, Role.STUDENT);
        User owner = userService.getById(actor.userId());
        Project project = projectRepository.save(new Project(
                newProject.title().trim(), newProject.description().trim(),
                newProject.techStack(), newProject.teamMembers(), owner));
        events.publishEvent(new ProjectCreatedEvent(project.getId(), actor.userId(), project.getTitle()));
        proposalService.createInitialDraft(project, proposal, actor);
        return buildView(project, actor);
    }

    /**
     * Business rule 1 (students see their own, faculty their assigned, the coordinator all projects),
     * narrowed by the optional filters in {@code query}. Only the coordinator may filter by the owner's
     * admission year or branch; anyone else passing them gets FORBIDDEN.
     */
    @Transactional(readOnly = true)
    public Page<ProjectListItem> listProjects(Actor actor, ProjectListQuery query) {
        if (query.filtersByCohort()) {
            accessPolicy.requireRole(actor, Role.COORDINATOR);
        }
        Page<Project> page = projectRepository.findAll(ProjectSpecifications.forQuery(actor, query), query.pageable());
        Map<UUID, Instant> submittedAt = proposalService.firstSubmittedAt(page.map(Project::getId).getContent());
        return page.map(project -> new ProjectListItem(project, submittedAt.get(project.getId())));
    }

    /** Dashboard counts over the same role scope as the list. */
    @Transactional(readOnly = true)
    public ProjectCounts countProjects(Actor actor) {
        List<ProjectStatusCount> rows = switch (actor.role()) {
            case STUDENT -> projectRepository.countByStatusForOwner(actor.userId());
            case FACULTY -> projectRepository.countByStatusForReviewer(actor.userId());
            case COORDINATOR -> projectRepository.countByStatus();
        };
        Map<ProjectStatus, Long> byStatus = new EnumMap<>(ProjectStatus.class);
        for (ProjectStatus status : ProjectStatus.values()) {
            byStatus.put(status, 0L);
        }
        rows.forEach(row -> byStatus.put(row.getStatus(), row.getCount()));
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        // A SUBMITTED project has no reviewer yet: assigning one moves it to UNDER_REVIEW.
        Long awaitingAssignment = actor.hasRole(Role.COORDINATOR) ? byStatus.get(ProjectStatus.SUBMITTED) : null;
        return new ProjectCounts(total, byStatus, awaitingAssignment);
    }

    @Transactional(readOnly = true)
    public ProjectView getProject(UUID projectId, Actor actor) {
        Project project = projectStore.getById(projectId);
        accessPolicy.requireView(project, actor);
        return buildView(project, actor);
    }

    private ProjectView buildView(Project project, Actor actor) {
        Optional<ProposalVersion> current = proposalService.findLatest(project.getId());
        boolean hasOpenDraft = current.map(ProposalVersion::isDraft).orElse(false);
        Integer latestSubmitted = proposalService.findLatestSubmitted(project.getId())
                .map(ProposalVersion::getVersionNumber)
                .orElse(null);
        return new ProjectView(project, current.orElse(null), latestSubmitted,
                allowedActionsResolver.resolve(project, actor, hasOpenDraft));
    }
}
