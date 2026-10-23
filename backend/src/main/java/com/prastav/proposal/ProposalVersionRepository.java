package com.prastav.proposal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository pattern: persistence for {@link ProposalVersion}. */
public interface ProposalVersionRepository extends JpaRepository<ProposalVersion, UUID> {

    List<ProposalVersion> findByProject_IdOrderByVersionNumberAsc(UUID projectId);

    Optional<ProposalVersion> findFirstByProject_IdOrderByVersionNumberDesc(UUID projectId);

    Optional<ProposalVersion> findFirstByProject_IdAndStateOrderByVersionNumberDesc(UUID projectId,
                                                                                     ProposalVersionState state);
}
