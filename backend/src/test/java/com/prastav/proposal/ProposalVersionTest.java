package com.prastav.proposal;

import com.prastav.common.ConflictException;
import com.prastav.project.Project;
import com.prastav.support.TestData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProposalVersionTest {

    private final Project project = TestData.project(TestData.student());
    private final ProposalVersionFactory factory = new ProposalVersionFactory();

    @Test
    void submittedVersionIsImmutable() {
        ProposalVersion version = TestData.submittedVersion(project, 1);

        assertThrows(ConflictException.class, () -> version.updateContent(ProposalContent.empty()));
        assertThrows(ConflictException.class, version::submit);
    }

    @Test
    void missingFieldsListsBlankRequiredFields() {
        ProposalVersion version = TestData.draft(project, 1,
                new ProposalContent("Problem", " ", null, "Outcome", "8 weeks", null));

        assertEquals(List.of("objectives", "methodology"), version.missingFields());
        assertFalse(version.isComplete());
    }

    @Test
    void referenceLinkIsOptional() {
        ProposalVersion version = TestData.draft(project, 1, new ProposalContent("p", "o", "m", "e", "t", null));
        assertTrue(version.isComplete());
    }

    @Test
    void factoryStartsAtOneAndIncrementsFromPrevious() {
        ProposalVersion v1 = factory.initialDraft(project, null, project.getOwner().getId());
        assertEquals(1, v1.getVersionNumber());
        assertTrue(v1.isDraft());

        v1.updateContent(TestData.completeContent());
        v1.submit();
        ProposalVersion v2 = factory.nextDraftFrom(v1, project.getOwner().getId());
        assertEquals(2, v2.getVersionNumber());
        assertEquals(v1.content(), v2.content());
    }

    @Test
    void factoryRefusesToReviseAnOpenDraft() {
        ProposalVersion draft = TestData.draft(project, 1, TestData.completeContent());
        assertThrows(ConflictException.class, () -> factory.nextDraftFrom(draft, project.getOwner().getId()));
    }
}
