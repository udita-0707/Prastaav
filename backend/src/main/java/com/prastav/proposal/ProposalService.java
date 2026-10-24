package com.prastav.proposal;

import com.prastav.auth.Actor;
import com.prastav.common.ConflictException;
import com.prastav.project.Project;
import com.prastav.project.ProjectAccessPolicy;
import com.prastav.project.ProjectStore;
import com.prastav.workflow.Action;
import com.prastav.workflow.IllegalTransitionException;
import com.prastav.workflow.ProjectStateFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Proposal versioning use cases: edit the open draft, start a revision, list versions.
 * Whether editing or revising is allowed in the current status is asked of the project's
 * {@code ProjectState}, so these rules stay consistent with the workflow table.
 */
@Service
public class ProposalService {

    private final ProposalVersionRepository versionRepository;
    private final ProposalVersionFactory versionFactory;
    private final ProjectStore projectStore;
    private final ProjectAccessPolicy accessPolicy;
    private final ProjectStateFactory stateFactory;
    private final ApplicationEventPublisher events;

    public ProposalService(ProposalVersionRepository versionRepository, ProposalVersionFactory versionFactory,
                           ProjectStore projectStore, ProjectAccessPolicy accessPolicy,
                           ProjectStateFactory stateFactory, ApplicationEventPublisher events) {
        this.versionRepository = versionRepository;
        this.versionFactory = versionFactory;
        this.projectStore = projectStore;
        this.accessPolicy = accessPolicy;
        this.stateFactory = stateFactory;
        this.events = events;
    }

    @Transactional
    public ProposalVersion createInitialDraft(Project project, ProposalContent content, Actor actor) {
        ProposalVersion first = versionRepository.save(versionFactory.initialDraft(project, content, actor.userId()));
        events.publishEvent(new ProposalVersionCreatedEvent(project.getId(), first.getVersionNumber(), actor.userId()));
        return first;
    }

    @Transactional
    public ProposalVersion updateDraft(UUID projectId, ProposalContent content, Actor actor) {
        Project project = projectStore.getById(projectId);
        accessPolicy.requireOwner(project, actor);
        requireStateAllows(project, Action.EDIT_DRAFT);
        ProposalVersion draft = findOpenDraft(projectId)
                .orElseThrow(() -> new ConflictException("There is no open draft; create a new version first"));
        draft.updateContent(content);
        ProposalVersion saved = versionRepository.save(draft);
        events.publishEvent(new ProposalDraftUpdatedEvent(projectId, saved.getVersionNumber(), actor.userId()));
        return saved;
    }

    /** FR-7: after REQUEST_REVISION the owner opens version N+1, prefilled from version N. */
    @Transactional
    public ProposalVersion createNextVersion(UUID projectId, Actor actor) {
        Project project = projectStore.getById(projectId);
        accessPolicy.requireOwner(project, actor);
        requireStateAllows(project, Action.CREATE_REVISION);
        findOpenDraft(projectId).ifPresent(open -> {
            throw new ConflictException("Version " + open.getVersionNumber() + " is already an open draft");
        });
        ProposalVersion latest = findLatest(projectId)
                .orElseThrow(() -> new ConflictException("Project has no proposal version to revise"));
        ProposalVersion next = versionRepository.save(versionFactory.nextDraftFrom(latest, actor.userId()));
        events.publishEvent(new ProposalVersionCreatedEvent(projectId, next.getVersionNumber(), actor.userId()));
        return next;
    }

    @Transactional(readOnly = true)
    public List<ProposalVersion> listVersions(UUID projectId, Actor actor) {
        Project project = projectStore.getById(projectId);
        accessPolicy.requireView(project, actor);
        return versionRepository.findByProject_IdOrderByVersionNumberAsc(projectId);
    }

    @Transactional(readOnly = true)
    public Optional<ProposalVersion> findOpenDraft(UUID projectId) {
        return versionRepository.findFirstByProject_IdAndStateOrderByVersionNumberDesc(
                projectId, ProposalVersionState.DRAFT);
    }

    @Transactional(readOnly = true)
    public Optional<ProposalVersion> findLatest(UUID projectId) {
        return versionRepository.findFirstByProject_IdOrderByVersionNumberDesc(projectId);
    }

    @Transactional(readOnly = true)
    public Optional<ProposalVersion> findLatestSubmitted(UUID projectId) {
        return versionRepository.findFirstByProject_IdAndStateOrderByVersionNumberDesc(
                projectId, ProposalVersionState.SUBMITTED);
    }

    /** First submission time per project, for list rows; projects never submitted are absent from the map. */
    @Transactional(readOnly = true)
    public Map<UUID, Instant> firstSubmittedAt(Collection<UUID> projectIds) {
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        return versionRepository.findFirstSubmissions(projectIds).stream()
                .collect(Collectors.toMap(FirstSubmission::getProjectId, FirstSubmission::getSubmittedAt));
    }

    private void requireStateAllows(Project project, Action action) {
        if (!stateFactory.stateOf(project.getStatus()).availableActions().contains(action)) {
            throw new IllegalTransitionException(project.getStatus(), action);
        }
    }
}
