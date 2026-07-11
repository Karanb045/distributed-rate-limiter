package com.rateshield.strategy;

import com.rateshield.entity.BucketState;
import com.rateshield.entity.ClientConfiguration;
import com.rateshield.enums.RateLimitAlgorithm;
import com.rateshield.enums.RateLimitStatus;
import com.rateshield.repository.BucketStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBucketStrategyTest {

    @Mock
    BucketStateRepository bucketStateRepository;

    @InjectMocks
    TokenBucketStrategy strategy;

    private ClientConfiguration config(int capacity, double rps) {
        return ClientConfiguration.builder()
                .clientKey("client-1")
                .algorithm(RateLimitAlgorithm.TOKEN_BUCKET)
                .requestsPerSecond(BigDecimal.valueOf(rps))
                .burstCapacity(capacity)
                .build();
    }

    @Test
    void allowsFirstRequestFromFullBucketAndConsumesOneToken() {
        when(bucketStateRepository.findByClientKey("client-1")).thenReturn(Optional.empty());
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

        RateLimitResult result = strategy.evaluate(config(10, 10), now);

        assertEquals(RateLimitStatus.ALLOW, result.status());
        assertEquals(9, result.remainingTokens());
        assertEquals(10, result.limit());
        assertEquals(0L, result.resetAfterMillis());
        assertEquals(RateLimitAlgorithm.TOKEN_BUCKET, result.algorithm());
        verify(bucketStateRepository).save(any(BucketState.class));
    }

    @Test
    void refillsTokensOverTimeAndCapsAtCapacity() {
        BucketState state = BucketState.createNew("client-1", BigDecimal.ZERO, LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        when(bucketStateRepository.findByClientKey("client-1")).thenReturn(Optional.of(state));
        // 5s at 10 rps => 50 tokens accrued, capped at capacity 10
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0, 5);

        RateLimitResult result = strategy.evaluate(config(10, 10), now);

        assertEquals(RateLimitStatus.ALLOW, result.status());
        ArgumentCaptor<BucketState> captor = ArgumentCaptor.forClass(BucketState.class);
        verify(bucketStateRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getTokensRemaining().compareTo(BigDecimal.valueOf(9)));
    }

    @Test
    void deniesWhenEmptyAndComputesPositiveReset() {
        BucketState state = BucketState.createNew("client-1", BigDecimal.ZERO, LocalDateTime.of(2026, 1, 1, 0, 0, 0));
        when(bucketStateRepository.findByClientKey("client-1")).thenReturn(Optional.of(state));
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

        RateLimitResult result = strategy.evaluate(config(10, 10), now);

        assertEquals(RateLimitStatus.DENY, result.status());
        assertEquals(0, result.remainingTokens());
        assertTrue(result.resetAfterMillis() > 0, "reset should be positive when denied");
        verify(bucketStateRepository, times(1)).save(any(BucketState.class));
    }
}
