package com.rateshield.repository;

import com.rateshield.entity.SlidingWindowRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Persistence layer for sliding window request timestamps.
 *
 * <p>Tracks individual request events so the algorithm can count requests
 * inside the rolling window and compute when the oldest event expires.
 */
@Repository
public interface SlidingWindowRequestRepository extends JpaRepository<SlidingWindowRequest, Long> {

    /**
     * Number of requests recorded for a client at or after the window start.
     */
    long countByClientKeyAndRequestTimestampGreaterThanEqual(String clientKey, LocalDateTime windowStart);

    /**
     * Removes request events older than the window start to bound table growth.
     *
     * @return the number of rows deleted
     */
    @Modifying
    int deleteByClientKeyAndRequestTimestampBefore(String clientKey, LocalDateTime windowStart);

    /**
     * Oldest request still inside the window, used to compute reset time.
     */
    Optional<SlidingWindowRequest> findFirstByClientKeyAndRequestTimestampGreaterThanEqualOrderByRequestTimestampAsc(
            String clientKey, LocalDateTime windowStart);
}
