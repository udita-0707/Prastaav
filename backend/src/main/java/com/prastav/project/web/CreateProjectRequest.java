package com.prastav.project.web;

import com.prastav.project.NewProject;
import com.prastav.proposal.ProposalContent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /api/projects: project metadata plus the (possibly partial) first draft. */
public record CreateProjectRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 4000) String description,
        @Size(max = 500) String techStack,
        @Size(max = 2000) String teamMembers,
        @Valid ProposalContent proposal) {

    public NewProject toNewProject() {
        return new NewProject(title, description, techStack, teamMembers);
    }

    public ProposalContent proposalOrEmpty() {
        return proposal == null ? ProposalContent.empty() : proposal;
    }
}
