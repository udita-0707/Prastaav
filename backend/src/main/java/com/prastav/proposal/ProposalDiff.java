package com.prastav.proposal;

import java.util.List;

/** Field-by-field comparison of two proposal versions (FR-14). A read model, not an entity. */
public record ProposalDiff(int fromVersion, int toVersion, List<FieldDiff> fields) {
}
