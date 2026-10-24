package com.prastav.project;

/** Metadata of a project being created (the proposal fields travel separately as ProposalContent). */
public record NewProject(String title, String description, String techStack, String teamMembers) {
}
