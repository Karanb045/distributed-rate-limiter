package com.rateshield.dto;

import com.rateshield.enums.RateLimitAlgorithm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for client configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientConfigResponse {

    private Long id;
    private String clientKey;
    private RateLimitAlgorithm algorithm;
    private BigDecimal requestsPerSecond;
    private Integer burstCapacity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}