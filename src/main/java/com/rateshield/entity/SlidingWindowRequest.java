package com.rateshield.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a single request timestamp for sliding window algorithm.
 * Used to calculate the number of requests in the current time window.
 */
@Entity
@Table(name = "sliding_window_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlidingWindowRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_key", nullable = false, length = 255)
    private String clientKey;

    @Column(name = "request_timestamp", nullable = false)
    private LocalDateTime requestTimestamp;

    public static SlidingWindowRequest createNew(String clientKey, LocalDateTime timestamp) {
        return SlidingWindowRequest.builder()
                .clientKey(clientKey)
                .requestTimestamp(timestamp)
                .build();
    }
}