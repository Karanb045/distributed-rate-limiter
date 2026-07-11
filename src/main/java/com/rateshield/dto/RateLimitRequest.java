package com.rateshield.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for checking rate limit.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitRequest {

    @NotBlank(message = "clientKey is required")
    private String clientKey;
}