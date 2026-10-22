package com.prastav.review;

import com.prastav.auth.Actor;
import com.prastav.workflow.WorkflowService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Strategy for REJECT: moves UNDER_REVIEW to REJECTED (terminal). Comments are mandatory. */
@Component
public class RejectDecisionHandler implements ReviewDecisionHandler {

    private final WorkflowService workflowService;

    public RejectDecisionHandler(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Override
    public ReviewDecision decision() {
        return ReviewDecision.REJECT;
    }

    @Override
    public void apply(UUID projectId, Actor actor, String comments) {
        workflowService.reject(projectId, actor, comments);
    }
}
