package com.prastav.project.web;

import com.prastav.auth.Actor;
import com.prastav.auth.JwtService;
import com.prastav.auth.RestAuthErrorHandler;
import com.prastav.auth.Role;
import com.prastav.auth.SecurityConfig;
import com.prastav.auth.User;
import com.prastav.project.ProjectHistoryService;
import com.prastav.project.ProjectService;
import com.prastav.workflow.Action;
import com.prastav.workflow.IllegalTransitionException;
import com.prastav.workflow.ProjectStatus;
import com.prastav.workflow.WorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controller slice: JWT authentication, role gates and the error-body mapping. */
@WebMvcTest(controllers = ProjectController.class)
@Import({SecurityConfig.class, JwtService.class, RestAuthErrorHandler.class})
class ProjectControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private WorkflowService workflowService;

    @MockBean
    private ProjectHistoryService projectHistoryService;

    private String bearer(Role role) {
        return "Bearer " + jwtService.generateToken(new User("Test " + role, role + "@prastav.edu", "hash", role));
    }

    @Test
    void requestWithoutTokenGets401WithErrorBody() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.path").value("/api/projects"));
    }

    @Test
    void studentCannotAssignReviewer() throws Exception {
        mockMvc.perform(post("/api/projects/{id}/assign", UUID.randomUUID())
                        .header("Authorization", bearer(Role.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"facultyId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void illegalTransitionMapsTo409() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(workflowService.submit(eq(projectId), any(Actor.class)))
                .thenThrow(new IllegalTransitionException(ProjectStatus.SUBMITTED, Action.SUBMIT));

        mockMvc.perform(post("/api/projects/{id}/submit", projectId)
                        .header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ILLEGAL_TRANSITION"))
                .andExpect(jsonPath("$.message").value("Action SUBMIT is not allowed while the project is SUBMITTED"));
    }
}
