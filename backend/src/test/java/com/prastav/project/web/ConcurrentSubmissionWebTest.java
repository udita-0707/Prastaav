package com.prastav.project.web;

import com.jayway.jsonpath.JsonPath;
import com.prastav.auth.Actor;
import com.prastav.auth.JwtService;
import com.prastav.auth.RestAuthErrorHandler;
import com.prastav.auth.SecurityConfig;
import com.prastav.auth.User;
import com.prastav.auth.UserService;
import com.prastav.project.Project;
import com.prastav.project.ProjectHistoryService;
import com.prastav.project.ProjectService;
import com.prastav.project.ProjectStore;
import com.prastav.project.ProjectView;
import com.prastav.proposal.ProposalService;
import com.prastav.support.TestData;
import com.prastav.workflow.ProjectStateFactory;
import com.prastav.workflow.ProjectStatus;
import com.prastav.workflow.ProjectStatusChangedEvent;
import com.prastav.workflow.ReviewerReassignedEvent;
import com.prastav.workflow.WorkflowService;
import jakarta.persistence.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Two requests racing for one project, through the real security chain, controller, {@link WorkflowService} and
 * error handler. The database is replaced by a store that behaves like {@code @Version} does: each request loads
 * its own copy of the project, the first save wins and any later save fails with the optimistic-lock exception.
 */
@WebMvcTest(controllers = ProjectController.class)
@Import({SecurityConfig.class, JwtService.class, RestAuthErrorHandler.class, WorkflowService.class,
        ConcurrentSubmissionWebTest.Config.class})
class ConcurrentSubmissionWebTest {

    private static final int TIMEOUT_SECONDS = 10;

    @TestConfiguration
    static class Config {

        @Bean
        ProjectStateFactory projectStateFactory() {
            return TestData.stateFactory();
        }

        @Bean
        PublishedEvents publishedEvents() {
            return new PublishedEvents();
        }
    }

    static class PublishedEvents {

        final List<Object> events = new CopyOnWriteArrayList<>();

        @EventListener
        void on(ProjectStatusChangedEvent event) {
            events.add(event);
        }

        @EventListener
        void on(ReviewerReassignedEvent event) {
            events.add(event);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PublishedEvents published;

    @MockBean
    private ProjectStore projectStore;

    @MockBean
    private ProposalService proposalService;

    @MockBean
    private UserService userService;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private ProjectHistoryService projectHistoryService;

    private final User owner = TestData.student();
    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        published.events.clear();
        when(projectService.getProject(eq(projectId), any(Actor.class)))
                .thenReturn(new ProjectView(TestData.project(owner), null, null, Set.of()));
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    @Test
    void twoSimultaneousSubmissionsGiveOneSuccessAndOneConcurrentUpdate() throws Exception {
        CyclicBarrier bothHaveLoaded = new CyclicBarrier(2);
        when(projectStore.getById(projectId)).thenAnswer(invocation -> TestData.project(owner));
        when(proposalService.findOpenDraft(projectId)).thenAnswer(invocation -> {
            bothHaveLoaded.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return Optional.of(TestData.draft(TestData.project(owner), 1, TestData.completeContent()));
        });
        AtomicBoolean committed = new AtomicBoolean();
        when(projectStore.save(any(Project.class))).thenAnswer(invocation -> {
            if (!committed.compareAndSet(false, true)) {
                throw new ObjectOptimisticLockingFailureException(Project.class, projectId);
            }
            return invocation.getArgument(0);
        });

        List<MockHttpServletResponse> responses = submitTwiceAtOnce();

        MockHttpServletResponse success = withStatus(responses, 200);
        MockHttpServletResponse conflict = withStatus(responses, 409);
        assertEquals("Smart Attendance", JsonPath.read(success.getContentAsString(), "$.title"));
        String body = conflict.getContentAsString();
        assertEquals("CONCURRENT_UPDATE", JsonPath.read(body, "$.code"));
        assertEquals(409, (int) JsonPath.read(body, "$.status"));
        assertEquals("/api/projects/" + projectId + "/submit", JsonPath.read(body, "$.path"));
        verify(projectStore, times(2)).save(any(Project.class));
        verify(projectService, times(1)).getProject(eq(projectId), any(Actor.class));
        assertEquals(1, published.events.size(), "the losing request must publish no event");
    }

    private List<MockHttpServletResponse> submitTwiceAtOnce() throws Exception {
        Callable<MockHttpServletResponse> submit = () -> mockMvc
                .perform(post("/api/projects/{id}/submit", projectId).header("Authorization", bearer(owner)))
                .andReturn().getResponse();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<MockHttpServletResponse>> running = List.of(pool.submit(submit), pool.submit(submit));
            return List.of(running.get(0).get(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    running.get(1).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    private static MockHttpServletResponse withStatus(List<MockHttpServletResponse> responses, int status) {
        List<MockHttpServletResponse> matching = responses.stream().filter(r -> r.getStatus() == status).toList();
        assertEquals(1, matching.size(), "expected exactly one response with status " + status);
        return matching.get(0);
    }

    @Test
    void aSubmissionArrivingAfterTheFirstCommittedIsAnIllegalTransition() throws Exception {
        when(projectStore.getById(projectId)).thenReturn(TestData.projectIn(ProjectStatus.SUBMITTED, owner, null));
        when(proposalService.findOpenDraft(projectId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/projects/{id}/submit", projectId).header("Authorization", bearer(owner)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ILLEGAL_TRANSITION"));

        assertTrue(published.events.isEmpty());
    }

    @Test
    void aReassignmentLosingARaceIsAConcurrentUpdate() throws Exception {
        User newReviewer = TestData.faculty();
        when(projectStore.getById(projectId))
                .thenReturn(TestData.projectIn(ProjectStatus.UNDER_REVIEW, owner, TestData.faculty()));
        when(userService.getById(newReviewer.getId())).thenReturn(newReviewer);
        when(projectStore.save(any(Project.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Project.class, projectId));

        mockMvc.perform(put("/api/projects/{id}/assign", projectId)
                        .header("Authorization", bearer(TestData.coordinator()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"facultyId\":\"" + newReviewer.getId() + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"));

        assertTrue(published.events.isEmpty());
    }

    @Test
    void theProjectVersionColumnIsWhatTheDatabaseChecks() throws NoSuchFieldException {
        assertTrue(Project.class.getDeclaredField("version").isAnnotationPresent(Version.class));
    }
}
