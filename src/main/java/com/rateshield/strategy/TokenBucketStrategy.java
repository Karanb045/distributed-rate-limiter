package com.rateshield.strategy;

import com.rateshield.entity.BucketState;
import com.rateshield.entity.ClientConfiguration;
import com.rateshield.enums.RateLimitAlgorithm;
import com.rateshield.enums.RateLimitStatus;
import com.rateshield.repository.BucketStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Token Bucket rate limiting strategy.
 *
 * <p>Tokens are refilled continuously at {@code requestsPerSecond} up to a
 * maximum of {@code burstCapacity}. Each allowed request consumes one token.
 * Bucket state is persisted so it survives application restarts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenBucketStrategy implements RateLimitStrategy {

    private final BucketStateRepository bucketStateRepository;

    private static final BigDecimal ONE = BigDecimal.ONE;

    @Override
    public RateLimitAlgorithm supportedAlgorithm() {
        return RateLimitAlgorithm.TOKEN_BUCKET;
    }

    @Override
    @Transactional
    public RateLimitResult evaluate(ClientConfiguration configuration, LocalDateTime now) {
        String clientKey = configuration.getClientKey();
        BigDecimal capacity = BigDecimal.valueOf(configuration.getBurstCapacity());
        BigDecimal refillRatePerSecond = configuration.getRequestsPerSecond();

        BucketState state = bucketStateRepository.findByClientKey(clientKey)
                .orElseGet(() -> BucketState.createNew(clientKey, capacity, now));

        BigDecimal tokens = refill(state, capacity, refillRatePerSecond, now);

        boolean allowed = tokens.compareTo(ONE) >= 0;
        int remainingTokens;
        long resetAfterMillis;
        if (allowed) {
            tokens = tokens.subtract(ONE);
            remainingTokens = floor(tokens);
            resetAfterMillis = 0L;
        } else {
            remainingTokens = 0;
            // Conservative estimate: time until at least one token is available.
            BigDecimal deficit = ONE.subtract(tokens);
            BigDecimal secondsToToken = deficit.divide(refillRatePerSecond, 10, RoundingMode.CEILING);
            resetAfterMillis = secondsToToken.multiply(BigDecimal.valueOf(1000)).longValue();
        }

        state.setTokensRemaining(tokens);
        state.setLastRefillTime(now);
        bucketStateRepository.save(state);

        log.debug("Token bucket [client={}] -> {} (remaining={})",
                clientKey, allowed ? "ALLOW" : "DENY", remainingTokens);

        return new RateLimitResult(
                allowed ? RateLimitStatus.ALLOW : RateLimitStatus.DENY,
                RateLimitAlgorithm.TOKEN_BUCKET,
                remainingTokens,
                configuration.getBurstCapacity(),
                resetAfterMillis,
                now
        );
    }

    /**
     * Adds tokens accrued since the last refill, capped at capacity.
     */
    private BigDecimal refill(BucketState state, BigDecimal capacity, BigDecimal refillRate, LocalDateTime now) {
        BigDecimal current = state.getTokensRemaining();
        long elapsedMillis = Duration.between(state.getLastRefillTime(), now).toMillis();
        if (elapsedMillis <= 0) {
            return current;
        }
        BigDecimal elapsedSeconds = BigDecimal.valueOf(elapsedMillis)
                .divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP);
        return current.add(refillRate.multiply(elapsedSeconds)).min(capacity);
    }

    private int floor(BigDecimal value) {
        return value.setScale(0, RoundingMode.FLOOR).intValue();
    }
}
