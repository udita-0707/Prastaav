package com.prastav.review;

import com.prastav.auth.Actor;
import com.prastav.workflow.WorkflowService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Strategy for APPROVE: moves the project UNDER_REVIEW to APPROVED. Comments are optional. */
@Component
public class ApproveDecisionHandler implements ReviewDecisionHandler {

    private final WorkflowService workflowService;

    public ApproveDecisionHandler(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Override
    public ReviewDecision decision() {
        return ReviewDecision.APPROVE;
    }

    @Override
    public void apply(UUID projectId, Actor actor, String comments) {
        workflowService.approve(projectId, actor);
    }
}
