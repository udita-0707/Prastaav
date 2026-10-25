package com.prastav.proposal.web;

import com.prastav.auth.Actor;
import com.prastav.auth.JwtService;
import com.prastav.auth.RestAuthErrorHandler;
import com.prastav.auth.Role;
import com.prastav.auth.SecurityConfig;
import com.prastav.auth.User;
import com.prastav.common.ForbiddenException;
import com.prastav.common.NotFoundException;
import com.prastav.common.ValidationException;
import com.prastav.proposal.FieldDiff;
import com.prastav.proposal.ProposalDiff;
import com.prastav.proposal.ProposalDiffService;
import com.prastav.proposal.ProposalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controller slice for the version comparison: binding, body shape and error mapping. */
@WebMvcTest(controllers = ProposalController.class)
@Import({SecurityConfig.class, JwtService.class, RestAuthErrorHandler.class})
class ProposalControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ProposalService proposalService;

    @MockBean
    private ProposalDiffService proposalDiffService;

    private final UUID projectId = UUID.randomUUID();

    private String bearer(Role role) {
        return "Bearer " + jwtService.generateToken(new User("Test " + role, role + "@prastav.edu", "hash", role));
    }

    @Test
    void compareReturnsVersionsAndFields() throws Exception {
        when(proposalDiffService.compare(eq(projectId), eq(1), eq(2), any(Actor.class))).thenReturn(new ProposalDiff(1, 2,
                List.of(new FieldDiff("objectives", "Digitise", "Digitise", false),
                        new FieldDiff("timeline", "8 weeks", "10 weeks", true))));

        mockMvc.perform(get("/api/projects/{id}/proposals/compare", projectId)
                        .param("from", "1").param("to", "2")
                        .header("Authorization", bearer(Role.FACULTY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromVersion").value(1))
                .andExpect(jsonPath("$.toVersion").value(2))
                .andExpect(jsonPath("$.fields[0].name").value("objectives"))
                .andExpect(jsonPath("$.fields[0].changed").value(false))
                .andExpect(jsonPath("$.fields[1].fromValue").value("8 weeks"))
                .andExpect(jsonPath("$.fields[1].toValue").value("10 weeks"))
                .andExpect(jsonPath("$.fields[1].changed").value(true));
    }

    @Test
    void versionOfAnotherProjectIs404() throws Exception {
        when(proposalDiffService.compare(eq(projectId), eq(1), eq(9), any(Actor.class)))
                .thenThrow(new NotFoundException("Proposal version", 9));

        mockMvc.perform(get("/api/projects/{id}/proposals/compare", projectId)
                        .param("from", "1").param("to", "9")
                        .header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Proposal version 9 not found"));
    }

    @Test
    void sameVersionIs400() throws Exception {
        when(proposalDiffService.compare(eq(projectId), eq(2), eq(2), any(Actor.class)))
                .thenThrow(new ValidationException("from and to must be different versions"));

        mockMvc.perform(get("/api/projects/{id}/proposals/compare", projectId)
                        .param("from", "2").param("to", "2")
                        .header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void nonParticipantIs403() throws Exception {
        when(proposalDiffService.compare(eq(projectId), eq(1), eq(2), any(Actor.class)))
                .thenThrow(new ForbiddenException("You are not a participant of this project"));

        mockMvc.perform(get("/api/projects/{id}/proposals/compare", projectId)
                        .param("from", "1").param("to", "2")
                        .header("Authorization", bearer(Role.FACULTY)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void missingOrNonNumericVersionIs400WithoutCallingTheService() throws Exception {
        mockMvc.perform(get("/api/projects/{id}/proposals/compare", projectId)
                        .param("from", "1")
                        .header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(get("/api/projects/{id}/proposals/compare", projectId)
                        .param("from", "one").param("to", "2")
                        .header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(proposalDiffService);
    }

    @Test
    void compareWithoutTokenIs401() throws Exception {
        mockMvc.perform(get("/api/projects/{id}/proposals/compare", projectId).param("from", "1").param("to", "2"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
