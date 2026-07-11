package com.rateshield.strategy;

import com.rateshield.entity.ClientConfiguration;
import com.rateshield.entity.SlidingWindowRequest;
import com.rateshield.enums.RateLimitAlgorithm;
import com.rateshield.enums.RateLimitStatus;
import com.rateshield.repository.SlidingWindowRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlidingWindowStrategyTest {

    @Mock
    SlidingWindowRequestRepository repository;

    @InjectMocks
    SlidingWindowStrategy strategy;

    private ClientConfiguration config(int capacity, double rps) {
        return ClientConfiguration.builder()
                .clientKey("client-1")
                .algorithm(RateLimitAlgorithm.SLIDING_WINDOW)
                .requestsPerSecond(BigDecimal.valueOf(rps))
                .burstCapacity(capacity)
                .build();
    }

    @Test
    void allowsUpToCapacityAndRecordsRequest() {
        when(repository.countByClientKeyAndRequestTimestampGreaterThanEqual(eq("client-1"), any())).thenReturn(0L);
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

        RateLimitResult result = strategy.evaluate(config(5, 5), now);

        assertEquals(RateLimitStatus.ALLOW, result.status());
        assertEquals(4, result.remainingTokens());
        verify(repository).save(any(SlidingWindowRequest.class));
    }

    @Test
    void deniesAtCapacityAndComputesResetFromOldestEvent() {
        when(repository.countByClientKeyAndRequestTimestampGreaterThanEqual(eq("client-1"), any())).thenReturn(5L);
        // window = 5/5 = 1s; oldest in-window event 0.5s ago => exits in 0.5s
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0, 5);
        LocalDateTime oldest = LocalDateTime.of(2026, 1, 1, 0, 0, 4).plusNanos(500_000_000L);
        when(repository.findFirstByClientKeyAndRequestTimestampGreaterThanEqualOrderByRequestTimestampAsc(eq("client-1"), any()))
                .thenReturn(Optional.of(SlidingWindowRequest.createNew("client-1", oldest)));

        RateLimitResult result = strategy.evaluate(config(5, 5), now);

        assertEquals(RateLimitStatus.DENY, result.status());
        assertEquals(0, result.remainingTokens());
        assertTrue(result.resetAfterMillis() > 0, "reset should be positive when denied");
        verify(repository, never()).save(any(SlidingWindowRequest.class));
    }

    @Test
    void windowExpiryFreesSlotAndAllowsAgain() {
        when(repository.deleteByClientKeyAndRequestTimestampBefore(eq("client-1"), any())).thenReturn(5);
        when(repository.countByClientKeyAndRequestTimestampGreaterThanEqual(eq("client-1"), any())).thenReturn(0L);
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0, 10);

        RateLimitResult result = strategy.evaluate(config(5, 5), now);

        assertEquals(RateLimitStatus.ALLOW, result.status());
    }

    @Test
    void eventExactlyAtWindowStartIsCounted() {
        when(repository.countByClientKeyAndRequestTimestampGreaterThanEqual(eq("client-1"), any())).thenReturn(5L);
        when(repository.findFirstByClientKeyAndRequestTimestampGreaterThanEqualOrderByRequestTimestampAsc(eq("client-1"), any()))
                .thenReturn(Optional.of(SlidingWindowRequest.createNew("client-1", LocalDateTime.of(2026, 1, 1, 0, 0, 4))));

        RateLimitResult result = strategy.evaluate(config(5, 5), LocalDateTime.of(2026, 1, 1, 0, 0, 5));

        assertEquals(RateLimitStatus.DENY, result.status());
        assertEquals(0, result.remainingTokens());
    }
}
