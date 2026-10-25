package com.prastav.project.web;

import com.prastav.auth.Actor;
import com.prastav.project.ProjectHistoryService;
import com.prastav.project.ProjectListItem;
import com.prastav.project.ProjectListQuery;
import com.prastav.project.ProjectService;
import com.prastav.project.ProjectSort;
import com.prastav.project.TimelineEntry;
import com.prastav.workflow.ProjectStatus;
import com.prastav.workflow.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** Project endpoints, including the two workflow actions triggered from the project page. HTTP only. */
@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projects")
public class ProjectController {

    private final ProjectService projectService;
    private final WorkflowService workflowService;
    private final ProjectHistoryService projectHistoryService;

    public ProjectController(ProjectService projectService, WorkflowService workflowService,
                             ProjectHistoryService projectHistoryService) {
        this.projectService = projectService;
        this.workflowService = workflowService;
        this.projectHistoryService = projectHistoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Create a project with draft proposal v1")
    public ProjectDetailResponse create(@Valid @RequestBody CreateProjectRequest request,
                                        @AuthenticationPrincipal Actor actor) {
        return ProjectMapper.toDetail(
                projectService.createProject(request.toNewProject(), request.proposalOrEmpty(), actor));
    }

    /** Header carrying the number of matching projects across all pages. */
    static final String TOTAL_COUNT_HEADER = "X-Total-Count";

    @GetMapping
    @Operation(summary = "Role-scoped project list with optional filters and paging",
            description = "Without page the whole filtered list is returned. X-Total-Count always holds the match count.")
    public ResponseEntity<List<ProjectSummaryResponse>> list(
            @Parameter(description = "One or more statuses (repeat the parameter or separate with commas)")
            @RequestParam(required = false) List<ProjectStatus> status,
            @Parameter(description = "Text matched against title, description, team, owner and reviewer names")
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID reviewerId,
            @RequestParam(required = false) ProjectSort sort,
            @Parameter(description = "Zero-based page index") @RequestParam(required = false) Integer page,
            @Parameter(description = "Page size, 1 to 100 (default 10)") @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal Actor actor) {
        ProjectListQuery query = new ProjectListQuery(
                status == null ? null : new HashSet<>(status), q, reviewerId, sort, page, size);
        Page<ProjectListItem> result = projectService.listProjects(actor, query);
        return ResponseEntity.ok()
                .header(TOTAL_COUNT_HEADER, String.valueOf(result.getTotalElements()))
                .body(result.map(ProjectMapper::toSummary).getContent());
    }

    @GetMapping("/summary")
    @Operation(summary = "Role-scoped project counts by status for the dashboards")
    public ProjectCountsResponse summary(@AuthenticationPrincipal Actor actor) {
        return ProjectMapper.toCounts(projectService.countProjects(actor));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Project detail with allowedActions")
    public ProjectDetailResponse get(@PathVariable UUID id, @AuthenticationPrincipal Actor actor) {
        return ProjectMapper.toDetail(projectService.getProject(id, actor));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Submit or resubmit the open draft (the current state decides)")
    public ProjectDetailResponse submit(@PathVariable UUID id, @AuthenticationPrincipal Actor actor) {
        workflowService.submit(id, actor);
        return ProjectMapper.toDetail(projectService.getProject(id, actor));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('COORDINATOR')")
    @Operation(summary = "Assign a faculty reviewer")
    public ProjectDetailResponse assign(@PathVariable UUID id, @Valid @RequestBody AssignReviewerRequest request,
                                        @AuthenticationPrincipal Actor actor) {
        workflowService.assignReviewer(id, request.facultyId(), actor);
        return ProjectMapper.toDetail(projectService.getProject(id, actor));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Merged timeline of versions, reviews and status changes")
    public List<TimelineEntry> history(@PathVariable UUID id, @AuthenticationPrincipal Actor actor) {
        return projectHistoryService.history(id, actor);
    }
}
