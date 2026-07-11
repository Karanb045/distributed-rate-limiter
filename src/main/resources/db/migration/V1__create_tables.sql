-- Create client_configuration table
CREATE TABLE client_configuration (
    id BIGSERIAL PRIMARY KEY,
    client_key VARCHAR(255) NOT NULL UNIQUE,
    algorithm VARCHAR(50) NOT NULL,
    requests_per_second DECIMAL(10, 2) NOT NULL,
    burst_capacity INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create bucket_state table for Token Bucket algorithm state persistence
CREATE TABLE bucket_state (
    id BIGSERIAL PRIMARY KEY,
    client_key VARCHAR(255) NOT NULL UNIQUE,
    tokens_remaining DECIMAL(10, 2) NOT NULL,
    last_refill_time TIMESTAMP NOT NULL,
    CONSTRAINT fk_bucket_client FOREIGN KEY (client_key) REFERENCES client_configuration(client_key) ON DELETE CASCADE
);

-- Create sliding_window_request table for Sliding Window algorithm state
CREATE TABLE sliding_window_request (
    id BIGSERIAL PRIMARY KEY,
    client_key VARCHAR(255) NOT NULL,
    request_timestamp TIMESTAMP NOT NULL,
    CONSTRAINT fk_window_client FOREIGN KEY (client_key) REFERENCES client_configuration(client_key) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_bucket_state_client_key ON bucket_state(client_key);
CREATE INDEX idx_sliding_window_client_key ON sliding_window_request(client_key);
CREATE INDEX idx_sliding_window_timestamp ON sliding_window_request(request_timestamp);
CREATE INDEX idx_client_key ON client_configuration(client_key);