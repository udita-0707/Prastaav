package com.prastav.project.web;

import com.prastav.auth.Actor;
import com.prastav.auth.JwtService;
import com.prastav.auth.RestAuthErrorHandler;
import com.prastav.auth.Role;
import com.prastav.auth.SecurityConfig;
import com.prastav.auth.User;
import com.prastav.common.ForbiddenException;
import com.prastav.project.Project;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Test
    void listBindsFiltersAndReturnsThePageEnvelope() throws Exception {
        UUID reviewerId = UUID.randomUUID();
        User owner = new User("Aarav Mehta", "aarav.23csai@nst.rishihood.edu.in", "hash", Role.STUDENT);
        Project project = new Project("Crop Monitoring", "Drone images", null, null, owner);
        ArgumentCaptor<ProjectListQuery> query = ArgumentCaptor.forClass(ProjectListQuery.class);
        when(projectService.listProjects(any(Actor.class), query.capture())).thenAnswer(invocation -> new PageImpl<>(
                List.of(new ProjectListItem(project, null)), invocation.<ProjectListQuery>getArgument(1).pageable(), 6));

        mockMvc.perform(get("/api/projects")
                        .param("status", "SUBMITTED,UNDER_REVIEW")
                        .param("q", "crop")
                        .param("reviewerId", reviewerId.toString())
                        .param("admissionYear", "2023")
                        .param("branch", "csai")
                        .param("sort", "OLDEST")
                        .param("page", "1")
                        .param("size", "5")
                        .header("Authorization", bearer(Role.COORDINATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Crop Monitoring"))
                .andExpect(jsonPath("$.content[0].owner.name").value("Aarav Mehta"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));

        ProjectListQuery bound = query.getValue();
        assertEquals(Set.of(ProjectStatus.SUBMITTED, ProjectStatus.UNDER_REVIEW), bound.statuses());
        assertEquals("crop", bound.q());
        assertEquals(reviewerId, bound.reviewerId());
        assertEquals(2023, bound.admissionYear());
        assertEquals("CSAI", bound.branch());
        assertEquals(ProjectSort.OLDEST, bound.sort());
        assertEquals(1, bound.page());
        assertEquals(5, bound.size());
    }

    @Test
    void listWithoutParametersAsksForTheFirstPageOfTenForTheCaller() throws Exception {
        ArgumentCaptor<Actor> actor = ArgumentCaptor.forClass(Actor.class);
        ArgumentCaptor<ProjectListQuery> query = ArgumentCaptor.forClass(ProjectListQuery.class);
        when(projectService.listProjects(actor.capture(), query.capture())).thenReturn(Page.empty(PageRequest.of(0, 10)));

        mockMvc.perform(get("/api/projects").header("Authorization", bearer(Role.FACULTY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        assertEquals(Role.FACULTY, actor.getValue().role());
        assertEquals(0, query.getValue().page());
        assertEquals(10, query.getValue().size());
        assertEquals(ProjectSort.LATEST, query.getValue().sort());
    }

    @ParameterizedTest
    @CsvSource({"status,PENDING", "sort,NEWEST", "page,-1", "page,first", "size,0", "size,51", "reviewerId,not-a-uuid", "admissionYear,twenty"})
    void invalidListParameterIsA400(String name, String value) throws Exception {
        mockMvc.perform(get("/api/projects").param(name, value).header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(projectService);
    }

    @Test
    void cohortFilterFromAStudentIsA403() throws Exception {
        when(projectService.listProjects(any(Actor.class), any(ProjectListQuery.class)))
                .thenThrow(new ForbiddenException("This action requires the COORDINATOR role"));

        mockMvc.perform(get("/api/projects").param("branch", "CSAI").header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
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
