package com.prastav.audit;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** The audit specifications against mocked JPA criteria: which column each filter constrains. */
@SuppressWarnings("unchecked")
class AuditSpecificationsTest {

    private static final Instant FROM = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-10-31T00:00:00Z");

    private Root<AuditLog> root;
    private CriteriaQuery<?> criteriaQuery;
    private CriteriaBuilder cb;
    private Path<Object> projectId;
    private Path<Object> actorId;
    private Path<Object> action;
    private Path<Instant> createdAt;
    private Predicate everything;
    private Predicate match;

    @BeforeEach
    void setUp() {
        root = mock(Root.class);
        criteriaQuery = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);
        projectId = mock(Path.class);
        actorId = mock(Path.class);
        action = mock(Path.class);
        createdAt = mock(Path.class);
        when(root.get("projectId")).thenReturn(projectId);
        when(root.get("actorId")).thenReturn(actorId);
        when(root.get("action")).thenReturn(action);
        when(root.<Instant>get("createdAt")).thenReturn(createdAt);
        everything = mock(Predicate.class);
        match = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(everything);
    }

    @Test
    void projectFilterConstrainsProjectId() {
        UUID id = UUID.randomUUID();
        when(cb.equal(projectId, id)).thenReturn(match);

        assertSame(match, AuditSpecifications.forProject(id).toPredicate(root, criteriaQuery, cb));
    }

    @Test
    void actorFilterConstrainsActorId() {
        UUID id = UUID.randomUUID();
        when(cb.equal(actorId, id)).thenReturn(match);

        assertSame(match, AuditSpecifications.byActor(id).toPredicate(root, criteriaQuery, cb));
    }

    @Test
    void actionFilterConstrainsAction() {
        when(cb.equal(action, AuditAction.REVIEWER_REASSIGNED)).thenReturn(match);

        assertSame(match, AuditSpecifications.withAction(AuditAction.REVIEWER_REASSIGNED)
                .toPredicate(root, criteriaQuery, cb));
    }

    @Test
    void fromIsAnInclusiveLowerBound() {
        when(cb.greaterThanOrEqualTo(createdAt, FROM)).thenReturn(match);

        assertSame(match, AuditSpecifications.createdFrom(FROM).toPredicate(root, criteriaQuery, cb));
    }

    @Test
    void toIsAnInclusiveUpperBound() {
        when(cb.lessThanOrEqualTo(createdAt, TO)).thenReturn(match);

        assertSame(match, AuditSpecifications.createdUntil(TO).toPredicate(root, criteriaQuery, cb));
    }

    @Test
    void missingValuesMatchEverything() {
        assertSame(everything, AuditSpecifications.forProject(null).toPredicate(root, criteriaQuery, cb));
        assertSame(everything, AuditSpecifications.byActor(null).toPredicate(root, criteriaQuery, cb));
        assertSame(everything, AuditSpecifications.withAction(null).toPredicate(root, criteriaQuery, cb));
        assertSame(everything, AuditSpecifications.createdFrom(null).toPredicate(root, criteriaQuery, cb));
        assertSame(everything, AuditSpecifications.createdUntil(null).toPredicate(root, criteriaQuery, cb));
        verifyNoInteractions(projectId, actorId, action, createdAt);
    }

    @Test
    void allFiltersCombineOnTheirOwnColumns() {
        UUID project = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        when(cb.equal(any(), any(Object.class))).thenReturn(match);
        when(cb.greaterThanOrEqualTo(createdAt, FROM)).thenReturn(match);
        when(cb.lessThanOrEqualTo(createdAt, TO)).thenReturn(match);
        when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(match);
        AuditQuery query = AuditQuery.of(project, actor, AuditAction.STATUS_CHANGED, FROM, TO, null, null);

        AuditSpecifications.forQuery(query).toPredicate(root, criteriaQuery, cb);

        verify(cb).equal(projectId, project);
        verify(cb).equal(actorId, actor);
        verify(cb).equal(action, AuditAction.STATUS_CHANGED);
        verify(cb).greaterThanOrEqualTo(createdAt, FROM);
        verify(cb).lessThanOrEqualTo(createdAt, TO);
        verify(cb, times(4)).and(any(Predicate.class), any(Predicate.class));
        verify(cb, never()).conjunction();
    }
}
