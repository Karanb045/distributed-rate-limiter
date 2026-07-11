package com.rateshield.strategy;

import com.rateshield.entity.ClientConfiguration;
import com.rateshield.entity.SlidingWindowRequest;
import com.rateshield.enums.RateLimitAlgorithm;
import com.rateshield.enums.RateLimitStatus;
import com.rateshield.repository.SlidingWindowRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Sliding Window (fixed rolling window) rate limiting strategy.
 *
 * <p>Allows at most {@code burstCapacity} requests within a rolling window whose
 * length is derived from {@code burstCapacity / requestsPerSecond}. Each request
 * records a timestamp; events older than the window are pruned to bound growth.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlidingWindowStrategy implements RateLimitStrategy {

    private final SlidingWindowRequestRepository slidingWindowRequestRepository;

    @Override
    public RateLimitAlgorithm supportedAlgorithm() {
        return RateLimitAlgorithm.SLIDING_WINDOW;
    }

    @Override
    @Transactional
    public RateLimitResult evaluate(ClientConfiguration configuration, LocalDateTime now) {
        String clientKey = configuration.getClientKey();
        int capacity = configuration.getBurstCapacity();

        BigDecimal windowSeconds = BigDecimal.valueOf(capacity)
                .divide(configuration.getRequestsPerSecond(), 10, RoundingMode.HALF_UP);
        long windowMillis = windowSeconds.multiply(BigDecimal.valueOf(1000)).longValue();
        LocalDateTime windowStart = now.minus(Duration.ofMillis(windowMillis));

        // Remove events that have aged out of the window.
        slidingWindowRequestRepository.deleteByClientKeyAndRequestTimestampBefore(clientKey, windowStart);

        long currentCount = slidingWindowRequestRepository
            .countByClientKeyAndRequestTimestampGreaterThanEqual(clientKey, windowStart);

        boolean allowed = currentCount < capacity;
        int remainingTokens;
        long resetAfterMillis;
        if (allowed) {
            slidingWindowRequestRepository.save(SlidingWindowRequest.createNew(clientKey, now));
            remainingTokens = capacity - (int) currentCount - 1;
            resetAfterMillis = 0L;
        } else {
            remainingTokens = 0;
            // Time until the oldest in-window event expires and frees a slot.
            LocalDateTime oldest = slidingWindowRequestRepository
                .findFirstByClientKeyAndRequestTimestampGreaterThanEqualOrderByRequestTimestampAsc(clientKey, windowStart)
                    .map(SlidingWindowRequest::getRequestTimestamp)
                    .orElse(now);
            long elapsedSinceOldest = Duration.between(oldest, now).toMillis();
            resetAfterMillis = Math.max(0L, windowMillis - elapsedSinceOldest);
        }

        log.debug("Sliding window [client={}] -> {} (count={}/{}, window={}ms)",
                clientKey, allowed ? "ALLOW" : "DENY", currentCount, capacity, windowMillis);

        return new RateLimitResult(
                allowed ? RateLimitStatus.ALLOW : RateLimitStatus.DENY,
                RateLimitAlgorithm.SLIDING_WINDOW,
                remainingTokens,
                capacity,
                resetAfterMillis,
                now
        );
    }
}
