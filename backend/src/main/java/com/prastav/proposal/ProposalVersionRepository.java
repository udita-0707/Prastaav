package com.prastav.proposal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository pattern: persistence for {@link ProposalVersion}. */
public interface ProposalVersionRepository extends JpaRepository<ProposalVersion, UUID> {

    List<ProposalVersion> findByProject_IdOrderByVersionNumberAsc(UUID projectId);

    Optional<ProposalVersion> findFirstByProject_IdOrderByVersionNumberDesc(UUID projectId);

    Optional<ProposalVersion> findByProject_IdAndVersionNumber(UUID projectId, int versionNumber);

    Optional<ProposalVersion> findFirstByProject_IdAndStateOrderByVersionNumberDesc(UUID projectId,
                                                                                     ProposalVersionState state);

    @Query("select v.project.id as projectId, min(v.submittedAt) as submittedAt from ProposalVersion v"
            + " where v.project.id in :projectIds and v.submittedAt is not null group by v.project.id")
    List<FirstSubmission> findFirstSubmissions(Collection<UUID> projectIds);
}
