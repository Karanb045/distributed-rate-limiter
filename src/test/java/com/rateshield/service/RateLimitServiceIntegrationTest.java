package com.rateshield.service;

import com.rateshield.dto.ClientConfigRequest;
import com.rateshield.dto.RateLimitResponse;
import com.rateshield.enums.RateLimitAlgorithm;
import com.rateshield.enums.RateLimitStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end rate limiting against the local PostgreSQL instance. Runs in a
 * rolled-back transaction so repeated runs stay idempotent.
 */
@SpringBootTest
@Transactional
class RateLimitServiceIntegrationTest {

    @Autowired
    ClientConfigurationService clientConfigurationService;

    @Autowired
    RateLimitService rateLimitService;

    @Test
    void tokenBucketAllowsBurstThenDenies() {
        clientConfigurationService.create(
                new ClientConfigRequest("tb-it-client", RateLimitAlgorithm.TOKEN_BUCKET, BigDecimal.valueOf(10), 3));

        assertEquals(RateLimitStatus.ALLOW, rateLimitService.check("tb-it-client").getStatus());
        assertEquals(RateLimitStatus.ALLOW, rateLimitService.check("tb-it-client").getStatus());
        assertEquals(RateLimitStatus.ALLOW, rateLimitService.check("tb-it-client").getStatus());
        RateLimitResponse denied = rateLimitService.check("tb-it-client");
        assertEquals(RateLimitStatus.DENY, denied.getStatus());
        assertEquals(0, denied.getRemainingTokens());
    }

    @Test
    void slidingWindowAllowsBurstThenDenies() {
        clientConfigurationService.create(
                new ClientConfigRequest("sw-it-client", RateLimitAlgorithm.SLIDING_WINDOW, BigDecimal.valueOf(10), 3));

        assertEquals(RateLimitStatus.ALLOW, rateLimitService.check("sw-it-client").getStatus());
        assertEquals(RateLimitStatus.ALLOW, rateLimitService.check("sw-it-client").getStatus());
        assertEquals(RateLimitStatus.ALLOW, rateLimitService.check("sw-it-client").getStatus());
        RateLimitResponse denied = rateLimitService.check("sw-it-client");
        assertEquals(RateLimitStatus.DENY, denied.getStatus());
    }
}
