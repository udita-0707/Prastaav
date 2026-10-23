package com.prastav.proposal;

import jakarta.validation.constraints.Size;

/**
 * The editable proposal fields as one value. Used as the request body for draft edits and copied into
 * a new version when a revision starts. Fields may be blank while drafting; completeness is checked on submit.
 */
public record ProposalContent(
        @Size(max = 4000) String problemStatement,
        @Size(max = 4000) String objectives,
        @Size(max = 4000) String methodology,
        @Size(max = 4000) String expectedOutcome,
        @Size(max = 2000) String timeline,
        @Size(max = 500) String referenceLink) {

    public static ProposalContent empty() {
        return new ProposalContent(null, null, null, null, null, null);
    }
}
