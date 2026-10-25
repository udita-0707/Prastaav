package com.prastav.project;

import com.prastav.auth.Actor;
import com.prastav.auth.Role;
import com.prastav.auth.User;
import com.prastav.workflow.ProjectStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The list specifications against mocked JPA criteria: which columns each role and filter constrains. */
@SuppressWarnings("unchecked")
class ProjectSpecificationsTest {

    private Root<Project> root;
    private CriteriaQuery<?> criteriaQuery;
    private CriteriaBuilder cb;
    private Path<Object> ownerId;
    private Path<Object> reviewerId;
    private Path<Object> status;
    private Predicate everything;

    @BeforeEach
    void setUp() {
        root = mock(Root.class);
        criteriaQuery = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);
        ownerId = mockPath(root, "owner");
        reviewerId = mockPath(root, "reviewer");
        status = mock(Path.class);
        when(root.get("status")).thenReturn(status);
        everything = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(everything);
    }

    private Path<Object> mockPath(Root<Project> from, String association) {
        Path<Object> parent = mock(Path.class);
        Path<Object> id = mock(Path.class);
        when(from.get(association)).thenReturn(parent);
        when(parent.get("id")).thenReturn(id);
        return id;
    }

    private static Actor actor(Role role) {
        return new Actor(UUID.randomUUID(), role);
    }

    @Test
    void studentSeesOnlyOwnProjects() {
        Actor student = actor(Role.STUDENT);
        Predicate own = mock(Predicate.class);
        when(cb.equal(ownerId, student.userId())).thenReturn(own);

        assertSame(own, ProjectSpecifications.visibleTo(student).toPredicate(root, criteriaQuery, cb));
        verify(root, never()).get("reviewer");
    }

    @Test
    void facultySeesOnlyAssignedProjects() {
        Actor faculty = actor(Role.FACULTY);
        Predicate assigned = mock(Predicate.class);
        when(cb.equal(reviewerId, faculty.userId())).thenReturn(assigned);

        assertSame(assigned, ProjectSpecifications.visibleTo(faculty).toPredicate(root, criteriaQuery, cb));
        verify(root, never()).get("owner");
    }

    @Test
    void coordinatorSeesEveryProject() {
        assertSame(everything, ProjectSpecifications.visibleTo(actor(Role.COORDINATOR)).toPredicate(root, criteriaQuery, cb));
    }

    @Test
    void emptyFiltersMatchEverything() {
        assertSame(everything, ProjectSpecifications.statusIn(Set.of()).toPredicate(root, criteriaQuery, cb));
        assertSame(everything, ProjectSpecifications.reviewedBy(null).toPredicate(root, criteriaQuery, cb));
        assertSame(everything, ProjectSpecifications.matches(null).toPredicate(root, criteriaQuery, cb));
    }

    @Test
    void ownerCohortConstrainsTheOwnersAdmissionYearAndBranch() {
        Path<Object> owner = (Path<Object>) root.get("owner");
        Path<Object> admissionYear = mock(Path.class);
        Path<Object> branch = mock(Path.class);
        when(owner.get("admissionYear")).thenReturn(admissionYear);
        when(owner.get("branch")).thenReturn(branch);
        Predicate year = mock(Predicate.class);
        Predicate inBranch = mock(Predicate.class);
        Predicate both = mock(Predicate.class);
        when(cb.equal(admissionYear, 2023)).thenReturn(year);
        when(cb.equal(branch, "CSAI")).thenReturn(inBranch);
        when(cb.and(year, inBranch)).thenReturn(both);

        assertSame(both, ProjectSpecifications.ownerCohort(2023, "CSAI").toPredicate(root, criteriaQuery, cb));
    }

    @Test
    void ownerCohortWithOnlyAYearLeavesTheBranchOpen() {
        Path<Object> owner = (Path<Object>) root.get("owner");
        Path<Object> admissionYear = mock(Path.class);
        when(owner.get("admissionYear")).thenReturn(admissionYear);
        Predicate year = mock(Predicate.class);
        when(cb.equal(admissionYear, 2024)).thenReturn(year);

        ProjectSpecifications.ownerCohort(2024, null).toPredicate(root, criteriaQuery, cb);

        verify(cb).and(year, everything);
        verify(owner, never()).get("branch");
    }

    @Test
    void studentQueryCombinesOwnerScopeStatusAndCaseInsensitiveSearch() {
        Actor student = actor(Role.STUDENT);
        Join<Project, User> owner = mock(Join.class);
        Join<Project, User> reviewer = mock(Join.class);
        when(root.<Project, User>join("owner", JoinType.INNER)).thenReturn(owner);
        when(root.<Project, User>join("reviewer", JoinType.LEFT)).thenReturn(reviewer);
        when(cb.lower(any())).thenReturn(mock(Expression.class));
        when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));
        Set<ProjectStatus> statuses = Set.of(ProjectStatus.DRAFT, ProjectStatus.SUBMITTED);
        ProjectListQuery query = ProjectListQuery.of(statuses, " Crop ", null, null, null, null, null, null);

        ProjectSpecifications.forQuery(student, query).toPredicate(root, criteriaQuery, cb);

        verify(cb).equal(ownerId, student.userId());
        verify(status).in(statuses);
        verify(root).get("title");
        verify(cb, times(5)).like(any(), eq("%crop%"), anyChar());
        verify(cb, never()).equal(eq(reviewerId), any(Object.class));
        verify(cb, never()).like(any(), anyString());
    }
}
