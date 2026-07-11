package com.rateshield.entity;

import com.rateshield.enums.RateLimitAlgorithm;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for a client's rate limit configuration.
 */
@Entity
@Table(name = "client_configuration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_key", nullable = false, unique = true, length = 255)
    private String clientKey;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private RateLimitAlgorithm algorithm;

    @Column(name = "requests_per_second", nullable = false, precision = 10, scale = 2)
    private BigDecimal requestsPerSecond;

    @Column(name = "burst_capacity", nullable = false)
    private Integer burstCapacity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}