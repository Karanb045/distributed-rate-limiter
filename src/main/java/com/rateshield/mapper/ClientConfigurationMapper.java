package com.rateshield.mapper;

import com.rateshield.dto.ClientConfigRequest;
import com.rateshield.dto.ClientConfigResponse;
import com.rateshield.entity.ClientConfiguration;
import org.springframework.stereotype.Component;

/**
 * Mapper for ClientConfiguration entity and DTOs.
 */
@Component
public class ClientConfigurationMapper {

    public ClientConfiguration toEntity(ClientConfigRequest request) {
        return ClientConfiguration.builder()
                .clientKey(request.getClientKey())
                .algorithm(request.getAlgorithm())
                .requestsPerSecond(request.getRequestsPerSecond())
                .burstCapacity(request.getBurstCapacity())
                .build();
    }

    public ClientConfigResponse toResponse(ClientConfiguration entity) {
        return ClientConfigResponse.builder()
                .id(entity.getId())
                .clientKey(entity.getClientKey())
                .algorithm(entity.getAlgorithm())
                .requestsPerSecond(entity.getRequestsPerSecond())
                .burstCapacity(entity.getBurstCapacity())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}