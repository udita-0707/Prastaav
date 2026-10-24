package com.prastav.project.web;

import com.prastav.auth.Actor;
import com.prastav.auth.JwtService;
import com.prastav.auth.RestAuthErrorHandler;
import com.prastav.auth.Role;
import com.prastav.auth.SecurityConfig;
import com.prastav.auth.User;
import com.prastav.project.ProjectCounts;
import com.prastav.project.ProjectHistoryService;
import com.prastav.project.ProjectListItem;
import com.prastav.project.ProjectListQuery;
import com.prastav.project.ProjectService;
import com.prastav.project.ProjectSort;
import com.prastav.workflow.Action;
import com.prastav.workflow.IllegalTransitionException;
import com.prastav.workflow.ProjectStatus;
import com.prastav.workflow.WorkflowService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @Test
    void listBindsFiltersAndReportsTotalCount() throws Exception {
        UUID reviewerId = UUID.randomUUID();
        ArgumentCaptor<ProjectListQuery> query = ArgumentCaptor.forClass(ProjectListQuery.class);
        when(projectService.listProjects(any(Actor.class), query.capture())).thenReturn(Page.<ProjectListItem>empty());

        mockMvc.perform(get("/api/projects")
                        .param("status", "SUBMITTED,UNDER_REVIEW")
                        .param("q", "crop")
                        .param("reviewerId", reviewerId.toString())
                        .param("sort", "OLDEST")
                        .param("page", "0")
                        .param("size", "5")
                        .header("Authorization", bearer(Role.COORDINATOR)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"))
                .andExpect(jsonPath("$").isArray());

        ProjectListQuery bound = query.getValue();
        assertEquals(Set.of(ProjectStatus.SUBMITTED, ProjectStatus.UNDER_REVIEW), bound.statuses());
        assertEquals("crop", bound.q());
        assertEquals(reviewerId, bound.reviewerId());
        assertEquals(ProjectSort.OLDEST, bound.sort());
        assertEquals(5, bound.pageable().getPageSize());
    }

    @Test
    void unknownStatusIsA400() throws Exception {
        mockMvc.perform(get("/api/projects").param("status", "PENDING").header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void summaryReturnsCountsByStatus() throws Exception {
        Map<ProjectStatus, Long> byStatus = new EnumMap<>(ProjectStatus.class);
        for (ProjectStatus status : ProjectStatus.values()) {
            byStatus.put(status, 0L);
        }
        byStatus.put(ProjectStatus.SUBMITTED, 2L);
        when(projectService.countProjects(any(Actor.class))).thenReturn(new ProjectCounts(2, byStatus, 2L));

        mockMvc.perform(get("/api/projects/summary").header("Authorization", bearer(Role.COORDINATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.byStatus.SUBMITTED").value(2))
                .andExpect(jsonPath("$.byStatus.DRAFT").value(0))
                .andExpect(jsonPath("$.awaitingAssignment").value(2));
    }
}
