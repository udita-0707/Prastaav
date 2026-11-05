package com.prastav.audit;

import com.prastav.auth.Actor;
import com.prastav.auth.Role;
import com.prastav.common.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuditQueryServiceTest {

    private AuditLogRepository repository;
    private AuditQueryService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuditLogRepository.class);
        service = new AuditQueryService(repository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void coordinatorReadsThePageTheRepositoryReturnsNewestFirst() {
        AuditLog entry = AuditLog.builder().actorId(UUID.randomUUID()).action(AuditAction.STATUS_CHANGED).build();
        Page<AuditLog> page = new PageImpl<>(List.of(entry));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<AuditLog> result = service.search(AuditQuery.of(null, null, null, null, null, 2, 5),
                new Actor(UUID.randomUUID(), Role.COORDINATOR));

        assertSame(page, result);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageable.capture());
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(5, pageable.getValue().getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getValue().getSort().getOrderFor("createdAt").getDirection());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"STUDENT", "FACULTY"})
    void otherRolesAreRejectedBeforeAnyRead(Role role) {
        AuditQuery query = AuditQuery.of(null, null, null, null, null, null, null);

        assertThrows(ForbiddenException.class, () -> service.search(query, new Actor(UUID.randomUUID(), role)));

        verifyNoInteractions(repository);
    }

    @Test
    void projectTimelineReadStaysAvailable() {
        UUID projectId = UUID.randomUUID();
        when(repository.findByProjectIdOrderByCreatedAtAsc(projectId)).thenReturn(List.of());

        assertEquals(List.of(), service.findByProject(projectId));
    }
}
