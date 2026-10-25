package com.prastav.proposal;

import com.prastav.auth.Actor;
import com.prastav.common.NotFoundException;
import com.prastav.common.ValidationException;
import com.prastav.project.Project;
import com.prastav.project.ProjectAccessPolicy;
import com.prastav.project.ProjectStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * FR-14: compares two versions of one project's proposal field by field, in the order the proposal form
 * shows them. Every field is returned, changed or not, so the UI can show both versions side by side.
 */
@Service
public class ProposalDiffService {

    private final ProposalVersionRepository versionRepository;
    private final ProjectStore projectStore;
    private final ProjectAccessPolicy accessPolicy;

    public ProposalDiffService(ProposalVersionRepository versionRepository, ProjectStore projectStore,
                               ProjectAccessPolicy accessPolicy) {
        this.versionRepository = versionRepository;
        this.projectStore = projectStore;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public ProposalDiff compare(UUID projectId, int fromVersion, int toVersion, Actor actor) {
        Project project = projectStore.getById(projectId);
        accessPolicy.requireView(project, actor);
        if (fromVersion == toVersion) {
            throw new ValidationException("from and to must be different versions");
        }
        ProposalContent from = load(projectId, fromVersion).content();
        ProposalContent to = load(projectId, toVersion).content();
        return new ProposalDiff(fromVersion, toVersion, List.of(
                field("problemStatement", from.problemStatement(), to.problemStatement()),
                field("objectives", from.objectives(), to.objectives()),
                field("methodology", from.methodology(), to.methodology()),
                field("expectedOutcome", from.expectedOutcome(), to.expectedOutcome()),
                field("timeline", from.timeline(), to.timeline()),
                field("referenceLink", from.referenceLink(), to.referenceLink())));
    }

    private ProposalVersion load(UUID projectId, int versionNumber) {
        return versionRepository.findByProject_IdAndVersionNumber(projectId, versionNumber)
                .orElseThrow(() -> new NotFoundException("Proposal version", versionNumber));
    }

    private static FieldDiff field(String name, String fromValue, String toValue) {
        return new FieldDiff(name, fromValue, toValue, !Objects.equals(fromValue, toValue));
    }
}
