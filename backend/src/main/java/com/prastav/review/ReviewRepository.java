package com.prastav.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Repository pattern: persistence for {@link Review}. */
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByProject_IdOrderByCreatedAtAsc(UUID projectId);
}
