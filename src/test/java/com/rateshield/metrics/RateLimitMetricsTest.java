package com.rateshield.metrics;

import com.rateshield.enums.RateLimitAlgorithm;
import com.rateshield.enums.RateLimitStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitMetricsTest {

    @Test
    void recordsCounterAndTimerForAllowedCheck() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RateLimitMetrics metrics = new RateLimitMetrics(registry);

        metrics.recordCheck("client-a", RateLimitAlgorithm.TOKEN_BUCKET, RateLimitStatus.ALLOW, 1_000_000L);

        double count = registry.counter("rateshield.ratelimit.checks",
                        "client", "client-a",
                        "algorithm", "TOKEN_BUCKET",
                        "status", "ALLOW")
                .count();
        assertThat(count).isEqualTo(1.0);

        assertThat(registry.find("rateshield.ratelimit.latency")
                .tag("client", "client-a")
                .tag("algorithm", "TOKEN_BUCKET")
                .timer())
                .isNotNull();
    }

    @Test
    void separatesCountsByStatus() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RateLimitMetrics metrics = new RateLimitMetrics(registry);

        metrics.recordCheck("client-b", RateLimitAlgorithm.SLIDING_WINDOW, RateLimitStatus.ALLOW, 500_000L);
        metrics.recordCheck("client-b", RateLimitAlgorithm.SLIDING_WINDOW, RateLimitStatus.DENY, 500_000L);

        double allowed = registry.counter("rateshield.ratelimit.checks",
                        "client", "client-b",
                        "algorithm", "SLIDING_WINDOW",
                        "status", "ALLOW")
                .count();
        double denied = registry.counter("rateshield.ratelimit.checks",
                        "client", "client-b",
                        "algorithm", "SLIDING_WINDOW",
                        "status", "DENY")
                .count();

        assertThat(allowed).isEqualTo(1.0);
        assertThat(denied).isEqualTo(1.0);
    }
}
