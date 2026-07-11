package com.rateshield.repository;

import com.rateshield.entity.BucketState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Persistence layer for token bucket state, allowing recovery of
 * remaining tokens and last refill time across application restarts.
 */
@Repository
public interface BucketStateRepository extends JpaRepository<BucketState, Long> {

    Optional<BucketState> findByClientKey(String clientKey);
}
