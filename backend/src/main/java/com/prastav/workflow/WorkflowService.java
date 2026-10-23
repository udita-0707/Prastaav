package com.prastav.workflow;

import com.prastav.auth.Actor;
import com.prastav.auth.User;
import com.prastav.auth.UserService;
import com.prastav.project.Project;
import com.prastav.project.ProjectStore;
import com.prastav.proposal.ProposalService;
import com.prastav.proposal.ProposalVersion;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * <b>Facade pattern.</b> The single entry point for every status change:
 * load project, get its state from {@link ProjectStateFactory}, run the action, save, publish
 * {@link ProjectStatusChangedEvent}. Controllers and other services never set a status themselves.
 * <p>
 * Why: callers see five simple methods instead of coordinating states, repositories and events,
 * and every transition goes through the same audited path in one transaction.
 */
@Service
public class WorkflowService {

    private final ProjectStore projectStore;
    private final ProjectStateFactory stateFactory;
    private final ProposalService proposalService;
    private final UserService userService;
    private final ApplicationEventPublisher events;

    public WorkflowService(ProjectStore projectStore, ProjectStateFactory stateFactory,
                           ProposalService proposalService, UserService userService,
                           ApplicationEventPublisher events) {
        this.projectStore = projectStore;
        this.stateFactory = stateFactory;
        this.proposalService = proposalService;
        this.userService = userService;
        this.events = events;
    }

    /** Submit (DRAFT) or resubmit (REVISION_REQUESTED); the current state decides which. */
    @Transactional
    public Project submit(UUID projectId, Actor actor) {
        Project project = projectStore.getById(projectId);
        ProposalVersion draft = proposalService.findOpenDraft(projectId).orElse(null);
        Map<String, String> details = draft == null
                ? Map.of()
                : Map.of("versionNumber", String.valueOf(draft.getVersionNumber()));
        return transition(project, actor, Action.SUBMIT, state -> state.submit(project, actor, draft), details);
    }

    @Transactional
    public Project assignReviewer(UUID projectId, UUID facultyId, Actor actor) {
        Project project = projectStore.getById(projectId);
        User faculty = userService.getById(facultyId);
        return transition(project, actor, Action.ASSIGN_REVIEWER,
                state -> state.assignReviewer(project, faculty, actor),
                Map.of("reviewerId", faculty.getId().toString(), "reviewerName", faculty.getName()));
    }

    @Transactional
    public Project approve(UUID projectId, Actor actor) {
        Project project = projectStore.getById(projectId);
        return transition(project, actor, Action.APPROVE, state -> state.approve(project, actor), Map.of());
    }

    @Transactional
    public Project requestRevision(UUID projectId, Actor actor, String comments) {
        Project project = projectStore.getById(projectId);
        return transition(project, actor, Action.REQUEST_REVISION,
                state -> state.requestRevision(project, actor, comments), Map.of());
    }

    @Transactional
    public Project reject(UUID projectId, Actor actor, String comments) {
        Project project = projectStore.getById(projectId);
        return transition(project, actor, Action.REJECT, state -> state.reject(project, actor, comments), Map.of());
    }

    private Project transition(Project project, Actor actor, Action action, Consumer<ProjectState> step,
                               Map<String, String> details) {
        ProjectStatus from = project.getStatus();
        step.accept(stateFactory.stateOf(from));
        Project saved = projectStore.save(project);
        events.publishEvent(new ProjectStatusChangedEvent(
                saved.getId(), actor.userId(), action, from, saved.getStatus(), details));
        return saved;
    }
}
