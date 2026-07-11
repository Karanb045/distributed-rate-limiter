-- Hardening: enforce positive rate-limit configuration values and add a
-- composite index that backs the sliding-window lookups (client_key + time).
ALTER TABLE client_configuration
    ADD CONSTRAINT chk_client_rps_positive CHECK (requests_per_second > 0),
    ADD CONSTRAINT chk_client_burst_positive CHECK (burst_capacity > 0);

CREATE INDEX IF NOT EXISTS idx_sliding_window_client_ts
    ON sliding_window_request (client_key, request_timestamp);
