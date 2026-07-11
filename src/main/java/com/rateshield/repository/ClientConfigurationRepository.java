package com.rateshield.repository;

import com.rateshield.entity.ClientConfiguration;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Persistence layer for client rate limit configurations.
 */
@Repository
public interface ClientConfigurationRepository extends JpaRepository<ClientConfiguration, Long> {

    Optional<ClientConfiguration> findByClientKey(String clientKey);

    // Pessimistic write lock serializes concurrent checks for the same client so the
    // token bucket / sliding window state is never double-counted under contention.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ClientConfiguration c where c.clientKey = :clientKey")
    Optional<ClientConfiguration> findByClientKeyForUpdate(@Param("clientKey") String clientKey);

    boolean existsByClientKey(String clientKey);

    long deleteByClientKey(String clientKey);
}
