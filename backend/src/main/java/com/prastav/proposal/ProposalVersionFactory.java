package com.prastav.proposal;

import com.prastav.common.ConflictException;
import com.prastav.project.Project;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * <b>Factory pattern.</b> The only place that creates {@link ProposalVersion} objects.
 * <p>
 * Why: version numbering (1, then previous + 1) and "prefill the revision from the previous version"
 * are creation rules; keeping them here means they cannot be bypassed or duplicated in services.
 */
@Component
public class ProposalVersionFactory {

    public ProposalVersion initialDraft(Project project, ProposalContent content, UUID createdBy) {
        return new ProposalVersion(project, 1, content == null ? ProposalContent.empty() : content, createdBy);
    }

    public ProposalVersion nextDraftFrom(ProposalVersion previous, UUID createdBy) {
        if (previous.isDraft()) {
            throw new ConflictException("Version " + previous.getVersionNumber() + " is still an open draft");
        }
        return new ProposalVersion(previous.getProject(), previous.getVersionNumber() + 1, previous.content(), createdBy);
    }
}
