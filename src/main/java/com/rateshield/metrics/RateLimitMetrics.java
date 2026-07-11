package com.rateshield.metrics;

import com.rateshield.enums.RateLimitAlgorithm;
import com.rateshield.enums.RateLimitStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Centralized Micrometer instrumentation for rate limit decisions.
 *
 * <p>Records one counter per (client, algorithm, status) and a latency timer per
 * (client, algorithm). Bytes are emitted through the standard
 * {@code /actuator/prometheus} endpoint already exposed by Spring Boot.
 *
 * <p>Note on cardinality: {@code client} is an unbounded tag, so in a real
 * deployment this should be bounded (e.g. only expose known/sampled clients) to
 * avoid Prometheus high-cardinality cost. Acceptable for this project's scale.
 */
@Component
public class RateLimitMetrics {

    private static final String CHECKS_COUNTER = "rateshield.ratelimit.checks";
    private static final String LATENCY_TIMER = "rateshield.ratelimit.latency";

    private final MeterRegistry registry;

    public RateLimitMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Record the outcome of a single rate limit evaluation.
     *
     * @param clientKey the client the request was evaluated for
     * @param algorithm the algorithm that made the decision
     * @param status    ALLOW or DENY
     * @param latencyNanos time taken to evaluate, in nanoseconds
     */
    public void recordCheck(String clientKey,
                            RateLimitAlgorithm algorithm,
                            RateLimitStatus status,
                            long latencyNanos) {
        registry.counter(CHECKS_COUNTER,
                        "client", clientKey,
                        "algorithm", algorithm.name(),
                        "status", status.name())
                .increment();

        Timer.builder(LATENCY_TIMER)
                .tag("client", clientKey)
                .tag("algorithm", algorithm.name())
                .description("Rate limit check latency")
                .register(registry)
                .record(latencyNanos, TimeUnit.NANOSECONDS);
    }
}
