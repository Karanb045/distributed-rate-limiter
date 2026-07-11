package com.rateshield.strategy;

import com.rateshield.entity.ClientConfiguration;
import com.rateshield.enums.RateLimitAlgorithm;

import java.time.LocalDateTime;

/**
 * Strategy contract for rate limiting algorithms.
 *
 * <p>Implementations decide whether a single request is allowed for a given
 * client configuration, persist any required state, and return the resulting
 * state as a {@link RateLimitResult}.
 */
public interface RateLimitStrategy {

    /**
     * @return the algorithm this strategy is responsible for.
     */
    RateLimitAlgorithm supportedAlgorithm();

    /**
     * Evaluate a single request against the algorithm.
     *
     * @param configuration the client rate limit configuration (must be persisted)
     * @param now           the current time, supplied by the caller for testability
     * @return the decision and resulting state
     */
    RateLimitResult evaluate(ClientConfiguration configuration, LocalDateTime now);
}
