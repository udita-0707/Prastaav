package com.prastav.audit.web;

import com.prastav.audit.AuditAction;
import com.prastav.audit.AuditLog;
import com.prastav.audit.AuditQuery;
import com.prastav.audit.AuditQueryService;
import com.prastav.auth.Actor;
import com.prastav.auth.JwtService;
import com.prastav.auth.RestAuthErrorHandler;
import com.prastav.auth.Role;
import com.prastav.auth.SecurityConfig;
import com.prastav.auth.User;
import com.prastav.workflow.ProjectStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controller slice: the audit log is coordinator-only, binds its filters and returns the page envelope. */
@WebMvcTest(controllers = AuditController.class)
@Import({SecurityConfig.class, JwtService.class, RestAuthErrorHandler.class})
class AuditControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private AuditQueryService auditQueryService;

    private String bearer(Role role) {
        return "Bearer " + jwtService.generateToken(new User("Test " + role, role + "@prastav.edu", "hash", role));
    }

    private AuditQuery capturedQuery() {
        ArgumentCaptor<AuditQuery> query = ArgumentCaptor.forClass(AuditQuery.class);
        verify(auditQueryService).search(query.capture(), any(Actor.class));
        return query.getValue();
    }

    private void returnsEmptyPage() {
        when(auditQueryService.search(any(AuditQuery.class), any(Actor.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
    }

    @Test
    void coordinatorGetsThePageEnvelopeWithEveryField() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        AuditLog entry = AuditLog.builder().projectId(projectId).actorId(actorId).action(AuditAction.STATUS_CHANGED)
                .fromStatus(ProjectStatus.SUBMITTED).toStatus(ProjectStatus.UNDER_REVIEW)
                .details("{\"action\":\"ASSIGN\"}").build();
        when(auditQueryService.search(any(AuditQuery.class), any(Actor.class)))
                .thenReturn(new PageImpl<>(List.of(entry), PageRequest.of(1, 2), 5));

        mockMvc.perform(get("/api/audit").header("Authorization", bearer(Role.COORDINATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(entry.getId().toString()))
                .andExpect(jsonPath("$.content[0].projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.content[0].actorId").value(actorId.toString()))
                .andExpect(jsonPath("$.content[0].action").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$.content[0].fromStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.content[0].toStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.content[0].details").value("{\"action\":\"ASSIGN\"}"))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void noParametersMeansNoFiltersAndDefaultPaging() throws Exception {
        returnsEmptyPage();

        mockMvc.perform(get("/api/audit").header("Authorization", bearer(Role.COORDINATOR)))
                .andExpect(status().isOk());

        AuditQuery query = capturedQuery();
        assertNull(query.projectId());
        assertNull(query.actorId());
        assertNull(query.action());
        assertNull(query.from());
        assertNull(query.to());
        assertEquals(0, query.page());
        assertEquals(20, query.size());
    }

    @Test
    void projectFilterIsBound() throws Exception {
        returnsEmptyPage();
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(get("/api/audit").param("projectId", projectId.toString())
                        .header("Authorization", bearer(Role.COORDINATOR)))
                .andExpect(status().isOk());

        assertEquals(projectId, capturedQuery().projectId());
    }

    @Test
    void actorFilterIsBound() throws Exception {
        returnsEmptyPage();
        UUID actorId = UUID.randomUUID();

        mockMvc.perform(get("/api/audit").param("actorId", actorId.toString())
                        .header("Authorization", bearer(Role.COORDINATOR)))
                .andExpect(status().isOk());

        assertEquals(actorId, capturedQuery().actorId());
    }

    @Test
    void actionFilterIsBound() throws Exception {
        returnsEmptyPage();

        mockMvc.perform(get("/api/audit").param("action", "REVIEW_RECORDED")
                        .header("Authorization", bearer(Role.COORDINATOR)))
                .andExpect(status().isOk());

        assertEquals(AuditAction.REVIEW_RECORDED, capturedQuery().action());
    }

    @Test
    void dateRangeIsBound() throws Exception {
        returnsEmptyPage();

        mockMvc.perform(get("/api/audit").param("from", "2026-10-01T00:00:00Z").param("to", "2026-10-31T23:59:59Z")
                        .header("Authorization", bearer(Role.COORDINATOR)))
                .andExpect(status().isOk());

        AuditQuery query = capturedQuery();
        assertEquals(Instant.parse("2026-10-01T00:00:00Z"), query.from());
        assertEquals(Instant.parse("2026-10-31T23:59:59Z"), query.to());
    }

    @Test
    void combinedFiltersAndPagingAreBoundTogether() throws Exception {
        returnsEmptyPage();
        UUID projectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        mockMvc.perform(get("/api/audit").param("projectId", projectId.toString())
                        .param("actorId", actorId.toString()).param("action", "STATUS_CHANGED")
                        .param("from", "2026-10-01T00:00:00Z").param("to", "2026-10-31T00:00:00Z")
                        .param("page", "2").param("size", "5")
                        .header("Authorization", bearer(Role.COORDINATOR)))
                .andExpect(status().isOk());

        assertEquals(new AuditQuery(projectId, actorId, AuditAction.STATUS_CHANGED,
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-31T00:00:00Z"), 2, 5), capturedQuery());
    }

    @ParameterizedTest
    @CsvSource({
            "page, -1",
            "page, abc",
            "size, 0",
            "size, 101",
            "projectId, not-a-uuid",
            "actorId, not-a-uuid",
            "action, NOT_AN_ACTION",
            "from, yesterday",
            "to, 2026-13-45"})
    void invalidParameterGets400WithoutReadingTheLog(String name, String value) throws Exception {
        mockMvc.perform(get("/api/audit").param(name, value).header("Authorization", bearer(Role.COORDINATOR)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(auditQueryService);
    }

    @Test
    void reversedDateRangeGets400() throws Exception {
        mockMvc.perform(get("/api/audit").param("from", "2026-10-31T00:00:00Z").param("to", "2026-10-01T00:00:00Z")
                        .header("Authorization", bearer(Role.COORDINATOR)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("from must not be after to"));

        verifyNoInteractions(auditQueryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"STUDENT", "FACULTY"})
    void otherRolesGet403(Role role) throws Exception {
        mockMvc.perform(get("/api/audit").header("Authorization", bearer(role)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verifyNoInteractions(auditQueryService);
    }

    @Test
    void requestWithoutTokenGets401() throws Exception {
        mockMvc.perform(get("/api/audit"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
