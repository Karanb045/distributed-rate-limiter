package com.rateshield.strategy;

import com.rateshield.enums.RateLimitAlgorithm;
import com.rateshield.enums.RateLimitStatus;

import java.time.LocalDateTime;

/**
 * Result returned by a {@link RateLimitStrategy} for a single rate limit evaluation.
 */
public record RateLimitResult(
        RateLimitStatus status,
        RateLimitAlgorithm algorithm,
        int remainingTokens,
        int limit,
        long resetAfterMillis,
        LocalDateTime timestamp
) {
}
