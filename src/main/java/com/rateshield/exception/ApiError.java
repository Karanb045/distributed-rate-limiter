package com.rateshield.exception;

import java.time.LocalDateTime;

/**
 * Stable API error contract returned by {@link GlobalExceptionHandler}.
 * `code` is a machine-readable identifier; `traceId` ties the response to the
 * request's correlation id (see {@code CorrelationFilter}) for log lookup.
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String code,
        String error,
        String message,
        String path,
        String traceId) {
}
