package com.prastav.support;

import com.prastav.auth.Actor;
import com.prastav.auth.Role;
import com.prastav.auth.User;
import com.prastav.project.Project;
import com.prastav.proposal.ProposalContent;
import com.prastav.proposal.ProposalVersion;
import com.prastav.workflow.ApprovedState;
import com.prastav.workflow.DraftState;
import com.prastav.workflow.ProjectStateFactory;
import com.prastav.workflow.ProjectStatus;
import com.prastav.workflow.RejectedState;
import com.prastav.workflow.RevisionRequestedState;
import com.prastav.workflow.SubmittedState;
import com.prastav.workflow.UnderReviewState;

import java.util.List;

/** Plain-Java fixtures shared by the unit tests. No Spring, no database. */
public final class TestData {

    private TestData() {
    }

    public static User student() {
        return new User("Aarav Mehta", "aarav@prastav.edu", "hash", Role.STUDENT);
    }

    public static User faculty() {
        return new User("Dr. Anita Rao", "anita.rao@prastav.edu", "hash", Role.FACULTY);
    }

    public static User coordinator() {
        return new User("Meera Nair", "coordinator@prastav.edu", "hash", Role.COORDINATOR);
    }

    public static Actor actor(User user) {
        return user.toActor();
    }

    public static ProposalContent completeContent() {
        return new ProposalContent("Paper-based approvals are slow", "Digitise the approval flow",
                "Spring Boot + React", "A working workflow system", "8 weeks", "https://example.org/ref");
    }

    public static Project project(User owner) {
        return new Project("Smart Attendance", "Face recognition attendance", "Java, React", "Aarav, Diya", owner);
    }

    /** A project already moved to {@code status}, with {@code reviewer} assigned when not null. */
    public static Project projectIn(ProjectStatus status, User owner, User reviewer) {
        Project project = project(owner);
        if (reviewer != null) {
            project.assignReviewer(reviewer);
        }
        project.moveTo(status);
        return project;
    }

    public static ProposalVersion draft(Project project, int versionNumber, ProposalContent content) {
        return new ProposalVersion(project, versionNumber, content, project.getOwner().getId());
    }

    public static ProposalVersion submittedVersion(Project project, int versionNumber) {
        ProposalVersion version = draft(project, versionNumber, completeContent());
        version.submit();
        return version;
    }

    public static ProjectStateFactory stateFactory() {
        return new ProjectStateFactory(List.of(new DraftState(), new SubmittedState(), new UnderReviewState(),
                new RevisionRequestedState(), new ApprovedState(), new RejectedState()));
    }
}
