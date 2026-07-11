package com.rateshield.dto;

import com.rateshield.enums.RateLimitAlgorithm;
import com.rateshield.enums.RateLimitStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for rate limit check.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitResponse {

    private RateLimitStatus status;
    private RateLimitAlgorithm algorithm;
    private Integer remainingTokens;
    private Integer limit;
    private Long resetAfterMillis;
    private LocalDateTime timestamp;
}