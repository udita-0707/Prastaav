package com.prastav.project;

import com.prastav.proposal.ProposalVersion;
import com.prastav.workflow.Action;

import java.util.Set;

/**
 * Read model for the project detail screen: the project, its newest proposal version, the number of the
 * latest submitted version and the actions this actor may take now.
 */
public record ProjectView(
        Project project,
        ProposalVersion currentProposal,
        Integer latestSubmittedVersion,
        Set<Action> allowedActions) {
}
