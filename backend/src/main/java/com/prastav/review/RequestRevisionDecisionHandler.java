package com.prastav.review;

import com.prastav.auth.Actor;
import com.prastav.workflow.WorkflowService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Strategy for REQUEST_REVISION: moves UNDER_REVIEW to REVISION_REQUESTED. Comments are mandatory. */
@Component
public class RequestRevisionDecisionHandler implements ReviewDecisionHandler {

    private final WorkflowService workflowService;

    public RequestRevisionDecisionHandler(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Override
    public ReviewDecision decision() {
        return ReviewDecision.REQUEST_REVISION;
    }

    @Override
    public void apply(UUID projectId, Actor actor, String comments) {
        workflowService.requestRevision(projectId, actor, comments);
    }
}
