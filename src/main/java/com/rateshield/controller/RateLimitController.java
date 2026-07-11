package com.rateshield.controller;

import com.rateshield.dto.RateLimitRequest;
import com.rateshield.dto.RateLimitResponse;
import com.rateshield.enums.RateLimitStatus;
import com.rateshield.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for evaluating rate limits.
 */
@RestController
@RequestMapping("/api/v1/rate-limit")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Rate Limit", description = "Evaluate rate limits for a client")
public class RateLimitController {

    private final RateLimitService service;

    @PostMapping("/check")
    @Operation(summary = "Check the rate limit for a client")
    public ResponseEntity<RateLimitResponse> check(@Valid @RequestBody RateLimitRequest request) {
        RateLimitResponse response = service.check(request.getClientKey());
        if (response.getStatus() == RateLimitStatus.DENY) {
            // Round the reset time up to whole seconds; clients must wait at least 1s.
            long retryAfterSeconds = Math.max(1L, (response.getResetAfterMillis() + 999) / 1000);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(retryAfterSeconds))
                    .body(response);
        }
        return ResponseEntity.ok(response);
    }
}
