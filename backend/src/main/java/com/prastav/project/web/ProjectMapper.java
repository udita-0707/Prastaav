package com.prastav.project.web;

import com.prastav.auth.web.UserMapper;
import com.prastav.project.Project;
import com.prastav.project.ProjectView;
import com.prastav.proposal.web.ProposalMapper;

import java.util.List;

/** DTO mapper for projects. */
public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static ProjectSummaryResponse toSummary(Project project) {
        return new ProjectSummaryResponse(project.getId(), project.getTitle(), project.getStatus(),
                UserMapper.toSummary(project.getOwner()), UserMapper.toSummary(project.getReviewer()),
                project.getUpdatedAt());
    }

    public static ProjectDetailResponse toDetail(ProjectView view) {
        Project project = view.project();
        return new ProjectDetailResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getTechStack(),
                project.getTeamMembers(),
                project.getStatus(),
                UserMapper.toSummary(project.getOwner()),
                UserMapper.toSummary(project.getReviewer()),
                view.currentProposal() == null ? null : ProposalMapper.toResponse(view.currentProposal()),
                view.latestSubmittedVersion(),
                List.copyOf(view.allowedActions()),
                project.getVersion(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
