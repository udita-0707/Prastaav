package com.prastav.proposal;

import com.prastav.auth.User;
import com.prastav.common.ConflictException;
import com.prastav.common.ForbiddenException;
import com.prastav.project.Project;
import com.prastav.project.ProjectAccessPolicy;
import com.prastav.project.ProjectStore;
import com.prastav.support.TestData;
import com.prastav.workflow.IllegalTransitionException;
import com.prastav.workflow.ProjectStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Versioning rules: sequential numbers, prefilled revisions, immutable submitted versions, one open draft. */
class ProposalServiceTest {

    private ProposalVersionRepository versionRepository;
    private ProjectStore projectStore;
    private ApplicationEventPublisher events;
    private ProposalService proposalService;

    private User owner;
    private User reviewer;

    @BeforeEach
    void setUp() {
        versionRepository = mock(ProposalVersionRepository.class);
        projectStore = mock(ProjectStore.class);
        events = mock(ApplicationEventPublisher.class);
        when(versionRepository.save(any(ProposalVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        proposalService = new ProposalService(versionRepository, new ProposalVersionFactory(), projectStore,
                new ProjectAccessPolicy(), TestData.stateFactory(), events);
        owner = TestData.student();
        reviewer = TestData.faculty();
    }

    private Project stored(Project project) {
        when(projectStore.getById(project.getId())).thenReturn(project);
        return project;
    }

    private void openDraft(Project project, ProposalVersion draft) {
        when(versionRepository.findFirstByProject_IdAndStateOrderByVersionNumberDesc(
                project.getId(), ProposalVersionState.DRAFT)).thenReturn(Optional.ofNullable(draft));
    }

    @Test
    void createNextVersionNumbersSequentiallyAndPrefillsFromPrevious() {
        Project project = stored(TestData.projectIn(ProjectStatus.REVISION_REQUESTED, owner, reviewer));
        ProposalVersion v1 = TestData.submittedVersion(project, 1);
        openDraft(project, null);
        when(versionRepository.findFirstByProject_IdOrderByVersionNumberDesc(project.getId())).thenReturn(Optional.of(v1));

        ProposalVersion v2 = proposalService.createNextVersion(project.getId(), TestData.actor(owner));

        assertEquals(2, v2.getVersionNumber());
        assertEquals(ProposalVersionState.DRAFT, v2.getState());
        assertEquals(v1.content(), v2.content());
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        assertEquals(2, ((ProposalVersionCreatedEvent) captor.getValue()).versionNumber());
    }

    @Test
    void editingTheNewVersionLeavesVersionOneUnchanged() {
        Project project = stored(TestData.projectIn(ProjectStatus.REVISION_REQUESTED, owner, reviewer));
        ProposalVersion v1 = TestData.submittedVersion(project, 1);
        ProposalContent original = v1.content();
        ProposalVersion v2 = new ProposalVersionFactory().nextDraftFrom(v1, owner.getId());
        openDraft(project, v2);
        ProposalContent revised = new ProposalContent("Sharper problem", "o", "m", "e", "t", null);

        proposalService.updateDraft(project.getId(), revised, TestData.actor(owner));

        assertEquals(revised, v2.content());
        assertEquals(original, v1.content());
        assertThrows(ConflictException.class, () -> v1.updateContent(revised));
    }

    @Test
    void onlyOneOpenDraftPerProject() {
        Project project = stored(TestData.projectIn(ProjectStatus.REVISION_REQUESTED, owner, reviewer));
        openDraft(project, TestData.draft(project, 2, TestData.completeContent()));

        assertThrows(ConflictException.class,
                () -> proposalService.createNextVersion(project.getId(), TestData.actor(owner)));
        verify(versionRepository, never()).save(any(ProposalVersion.class));
    }

    @Test
    void revisionCannotStartWhileUnderReview() {
        Project project = stored(TestData.projectIn(ProjectStatus.UNDER_REVIEW, owner, reviewer));

        assertThrows(IllegalTransitionException.class,
                () -> proposalService.createNextVersion(project.getId(), TestData.actor(owner)));
    }

    @Test
    void submittedProposalCannotBeEdited() {
        Project project = stored(TestData.projectIn(ProjectStatus.SUBMITTED, owner, null));

        assertThrows(IllegalTransitionException.class,
                () -> proposalService.updateDraft(project.getId(), TestData.completeContent(), TestData.actor(owner)));
    }

    @Test
    void onlyOwnerCanEditDraft() {
        Project project = stored(TestData.project(owner));
        openDraft(project, TestData.draft(project, 1, TestData.completeContent()));

        assertThrows(ForbiddenException.class, () -> proposalService.updateDraft(
                project.getId(), TestData.completeContent(), TestData.actor(TestData.student())));
    }

    @Test
    void ownerEditsOpenDraftInDraftStatus() {
        Project project = stored(TestData.project(owner));
        ProposalVersion v1 = TestData.draft(project, 1, ProposalContent.empty());
        openDraft(project, v1);

        ProposalVersion saved = proposalService.updateDraft(project.getId(), TestData.completeContent(),
                TestData.actor(owner));

        assertEquals(TestData.completeContent(), saved.content());
        verify(events).publishEvent(any(ProposalDraftUpdatedEvent.class));
    }
}
