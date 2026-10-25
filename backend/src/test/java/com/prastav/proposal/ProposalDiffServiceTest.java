package com.prastav.proposal;

import com.prastav.auth.Role;
import com.prastav.auth.User;
import com.prastav.common.ForbiddenException;
import com.prastav.common.NotFoundException;
import com.prastav.common.ValidationException;
import com.prastav.project.Project;
import com.prastav.project.ProjectAccessPolicy;
import com.prastav.project.ProjectStore;
import com.prastav.support.TestData;
import com.prastav.workflow.ProjectStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Version comparison: every field in form order, changed flags, version ownership and participant access. */
class ProposalDiffServiceTest {

    private ProposalVersionRepository versionRepository;
    private ProjectStore projectStore;
    private ProposalDiffService diffService;

    private User owner;
    private User reviewer;
    private Project project;

    @BeforeEach
    void setUp() {
        versionRepository = mock(ProposalVersionRepository.class);
        projectStore = mock(ProjectStore.class);
        diffService = new ProposalDiffService(versionRepository, projectStore, new ProjectAccessPolicy());
        owner = TestData.student();
        reviewer = TestData.faculty();
        project = TestData.projectIn(ProjectStatus.UNDER_REVIEW, owner, reviewer);
        when(projectStore.getById(project.getId())).thenReturn(project);
    }

    private void stored(ProposalVersion version) {
        when(versionRepository.findByProject_IdAndVersionNumber(project.getId(), version.getVersionNumber()))
                .thenReturn(Optional.of(version));
    }

    @Test
    void returnsEveryFieldInFormOrderWithChangedFlags() {
        stored(TestData.submittedVersion(project, 1));
        ProposalContent revised = new ProposalContent("Paper-based approvals are slow", "Digitise the approval flow",
                "Spring Boot + React + PostgreSQL", "A working workflow system", "10 weeks", null);
        stored(TestData.draft(project, 2, revised));

        ProposalDiff diff = diffService.compare(project.getId(), 1, 2, TestData.actor(reviewer));

        assertEquals(1, diff.fromVersion());
        assertEquals(2, diff.toVersion());
        assertEquals(List.of("problemStatement", "objectives", "methodology", "expectedOutcome", "timeline",
                "referenceLink"), diff.fields().stream().map(FieldDiff::name).toList());
        assertEquals(List.of(false, false, true, false, true, true),
                diff.fields().stream().map(FieldDiff::changed).toList());
        FieldDiff methodology = diff.fields().get(2);
        assertEquals("Spring Boot + React", methodology.fromValue());
        assertEquals("Spring Boot + React + PostgreSQL", methodology.toValue());
        assertNull(diff.fields().get(5).toValue());
    }

    @Test
    void identicalContentHasNoChangedFieldAndOrderCanBeReversed() {
        stored(TestData.submittedVersion(project, 1));
        stored(TestData.submittedVersion(project, 2));

        ProposalDiff diff = diffService.compare(project.getId(), 2, 1, TestData.actor(owner));

        assertEquals(2, diff.fromVersion());
        assertEquals(1, diff.toVersion());
        assertTrue(diff.fields().stream().noneMatch(FieldDiff::changed));
        assertEquals(diff.fields().get(0).fromValue(), diff.fields().get(0).toValue());
    }

    @Test
    void versionNotInTheProjectIs404() {
        stored(TestData.submittedVersion(project, 1));
        when(versionRepository.findByProject_IdAndVersionNumber(project.getId(), 7)).thenReturn(Optional.empty());

        NotFoundException error = assertThrows(NotFoundException.class,
                () -> diffService.compare(project.getId(), 1, 7, TestData.actor(owner)));
        assertEquals("Proposal version 7 not found", error.getMessage());
    }

    @Test
    void sameVersionOnBothSidesIsAValidationError() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> diffService.compare(project.getId(), 1, 1, TestData.actor(owner)));
        assertEquals("from and to must be different versions", error.getMessage());
        verify(versionRepository, never()).findByProject_IdAndVersionNumber(any(), anyInt());
    }

    @Test
    void nonParticipantIsForbiddenBeforeAnyVersionIsRead() {
        User otherFaculty = new User("Dr. Kabir Sen", "kabir.sen@prastav.edu", "hash", Role.FACULTY);

        assertThrows(ForbiddenException.class,
                () -> diffService.compare(project.getId(), 1, 2, TestData.actor(TestData.student())));
        assertThrows(ForbiddenException.class,
                () -> diffService.compare(project.getId(), 1, 2, TestData.actor(otherFaculty)));
        verify(versionRepository, never()).findByProject_IdAndVersionNumber(any(), anyInt());
    }

    @Test
    void coordinatorCanCompareAnyProject() {
        stored(TestData.submittedVersion(project, 1));
        stored(TestData.draft(project, 2, ProposalContent.empty()));

        ProposalDiff diff = diffService.compare(project.getId(), 1, 2, TestData.actor(TestData.coordinator()));

        assertFalse(diff.fields().isEmpty());
        assertTrue(diff.fields().stream().allMatch(FieldDiff::changed));
    }
}
