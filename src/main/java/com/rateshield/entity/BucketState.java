package com.rateshield.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing the state of a token bucket for a client.
 * Persists tokens remaining and last refill timestamp for recovery on restart.
 */
@Entity
@Table(name = "bucket_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BucketState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_key", nullable = false, unique = true, length = 255)
    private String clientKey;

    @Column(name = "tokens_remaining", nullable = false, precision = 10, scale = 2)
    private BigDecimal tokensRemaining;

    @Column(name = "last_refill_time", nullable = false)
    private LocalDateTime lastRefillTime;

    public static BucketState createNew(String clientKey, BigDecimal tokens, LocalDateTime now) {
        return BucketState.builder()
                .clientKey(clientKey)
                .tokensRemaining(tokens)
                .lastRefillTime(now)
                .build();
    }
}