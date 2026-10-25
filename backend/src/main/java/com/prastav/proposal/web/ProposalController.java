package com.prastav.proposal.web;

import com.prastav.auth.Actor;
import com.prastav.proposal.ProposalContent;
import com.prastav.proposal.ProposalDiff;
import com.prastav.proposal.ProposalDiffService;
import com.prastav.proposal.ProposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Proposal version endpoints under a project. HTTP only. */
@RestController
@RequestMapping("/api/projects/{projectId}")
@Tag(name = "Proposals")
public class ProposalController {

    private final ProposalService proposalService;
    private final ProposalDiffService proposalDiffService;

    public ProposalController(ProposalService proposalService, ProposalDiffService proposalDiffService) {
        this.proposalService = proposalService;
        this.proposalDiffService = proposalDiffService;
    }

    @PutMapping("/proposal")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Edit the open draft version")
    public ProposalVersionResponse updateDraft(@PathVariable UUID projectId,
                                               @Valid @RequestBody ProposalContent content,
                                               @AuthenticationPrincipal Actor actor) {
        return ProposalMapper.toResponse(proposalService.updateDraft(projectId, content, actor));
    }

    @PostMapping("/proposals")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Create the next version draft (only in REVISION_REQUESTED)")
    public ProposalVersionResponse createNextVersion(@PathVariable UUID projectId,
                                                     @AuthenticationPrincipal Actor actor) {
        return ProposalMapper.toResponse(proposalService.createNextVersion(projectId, actor));
    }

    @GetMapping("/proposals")
    @Operation(summary = "All versions of the proposal, oldest first")
    public List<ProposalVersionResponse> list(@PathVariable UUID projectId, @AuthenticationPrincipal Actor actor) {
        return proposalService.listVersions(projectId, actor).stream().map(ProposalMapper::toResponse).toList();
    }

    @GetMapping("/proposals/compare")
    @Operation(summary = "Field-by-field comparison of two versions of the proposal",
            description = "Project participants only. Both versions must belong to the project (404 otherwise); "
                    + "from and to must differ (VALIDATION_FAILED).")
    public ProposalDiff compare(@PathVariable UUID projectId,
                                @Parameter(description = "Version number to compare from") @RequestParam int from,
                                @Parameter(description = "Version number to compare to") @RequestParam int to,
                                @AuthenticationPrincipal Actor actor) {
        return proposalDiffService.compare(projectId, from, to, actor);
    }
}
