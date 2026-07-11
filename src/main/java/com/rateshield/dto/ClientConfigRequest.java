package com.rateshield.dto;

import com.rateshield.enums.RateLimitAlgorithm;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating/updating client configuration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientConfigRequest {

    @NotBlank(message = "clientKey is required")
    private String clientKey;

    @NotNull(message = "algorithm is required")
    private RateLimitAlgorithm algorithm;

    @NotNull(message = "requestsPerSecond is required")
    @DecimalMin(value = "0.01", message = "requestsPerSecond must be at least 0.01")
    private BigDecimal requestsPerSecond;

    @NotNull(message = "burstCapacity is required")
    @Min(value = 1, message = "burstCapacity must be at least 1")
    private Integer burstCapacity;
}