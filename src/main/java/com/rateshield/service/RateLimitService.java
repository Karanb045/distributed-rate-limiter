package com.rateshield.service;

import com.rateshield.dto.RateLimitResponse;
import com.rateshield.entity.ClientConfiguration;
import com.rateshield.enums.RateLimitAlgorithm;
import com.rateshield.exception.ClientNotFoundException;
import com.rateshield.repository.ClientConfigurationRepository;
import com.rateshield.metrics.RateLimitMetrics;
import com.rateshield.strategy.RateLimitResult;
import com.rateshield.strategy.RateLimitStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Service that evaluates rate limits for a client by delegating to the
 * configured {@link RateLimitStrategy}.
 *
 * <p>Strategies are discovered via Spring and indexed by
 * {@link RateLimitAlgorithm}, so adding a new algorithm requires no change here.
 */
@Service
@Slf4j
public class RateLimitService {

    private final ClientConfigurationRepository clientConfigurationRepository;
    private final Map<RateLimitAlgorithm, RateLimitStrategy> strategies;
    private final RateLimitMetrics rateLimitMetrics;

    public RateLimitService(ClientConfigurationRepository clientConfigurationRepository,
                            List<RateLimitStrategy> strategyList,
                            RateLimitMetrics rateLimitMetrics) {
        this.clientConfigurationRepository = clientConfigurationRepository;
        this.strategies = new EnumMap<>(RateLimitAlgorithm.class);
        for (RateLimitStrategy strategy : strategyList) {
            strategies.put(strategy.supportedAlgorithm(), strategy);
        }
        this.rateLimitMetrics = rateLimitMetrics;
    }

    /**
     * Evaluate a single request for the given client key.
     *
     * @param clientKey the client identifier
     * @return the rate limit decision and resulting state
     * @throws ClientNotFoundException if no configuration exists for the key
     */
    @org.springframework.transaction.annotation.Transactional
    public RateLimitResponse check(String clientKey) {
        ClientConfiguration config = clientConfigurationRepository.findByClientKeyForUpdate(clientKey)
                .orElseThrow(() -> new ClientNotFoundException(clientKey));

        RateLimitStrategy strategy = strategies.get(config.getAlgorithm());
        if (strategy == null) {
            throw new IllegalStateException(
                    "No rate limit strategy registered for algorithm: " + config.getAlgorithm());
        }

        long startNanos = System.nanoTime();
        RateLimitResult result = strategy.evaluate(config, LocalDateTime.now());
        long latencyNanos = System.nanoTime() - startNanos;
        rateLimitMetrics.recordCheck(clientKey, config.getAlgorithm(), result.status(), latencyNanos);
        log.debug("Rate limit check [client={}] -> {}", clientKey, result.status());

        return RateLimitResponse.builder()
                .status(result.status())
                .algorithm(result.algorithm())
                .remainingTokens(result.remainingTokens())
                .limit(result.limit())
                .resetAfterMillis(result.resetAfterMillis())
                .timestamp(result.timestamp())
                .build();
    }
}
