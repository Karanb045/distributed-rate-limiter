package com.rateshield.service;

import com.rateshield.dto.ClientConfigRequest;
import com.rateshield.dto.ClientConfigResponse;
import com.rateshield.entity.ClientConfiguration;
import com.rateshield.exception.ClientNotFoundException;
import com.rateshield.exception.DuplicateClientException;
import com.rateshield.mapper.ClientConfigurationMapper;
import com.rateshield.repository.ClientConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing client rate limit configurations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientConfigurationService {

    private final ClientConfigurationRepository repository;
    private final ClientConfigurationMapper mapper;

    @Transactional
    public ClientConfigResponse create(ClientConfigRequest request) {
        String clientKey = request.getClientKey();
        if (repository.existsByClientKey(clientKey)) {
            throw new DuplicateClientException(clientKey);
        }
        try {
            ClientConfiguration saved = repository.saveAndFlush(mapper.toEntity(request));
            log.info("Created client configuration for key={} algorithm={}",
                    clientKey, saved.getAlgorithm());
            return mapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateClientException(clientKey);
        }
    }

    @Transactional
    public ClientConfigResponse update(String clientKey, ClientConfigRequest request) {
        ClientConfiguration existing = repository.findByClientKey(clientKey)
                .orElseThrow(() -> new ClientNotFoundException(clientKey));

        existing.setAlgorithm(request.getAlgorithm());
        existing.setRequestsPerSecond(request.getRequestsPerSecond());
        existing.setBurstCapacity(request.getBurstCapacity());
        // clientKey is the immutable identity; it is intentionally not changed.
        ClientConfiguration saved = repository.save(existing);
        log.info("Updated client configuration for key={}", clientKey);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ClientConfigResponse getByKey(String clientKey) {
        return repository.findByClientKey(clientKey)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ClientNotFoundException(clientKey));
    }

    @Transactional(readOnly = true)
    public List<ClientConfigResponse> getAll() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public void delete(String clientKey) {
        if (!repository.existsByClientKey(clientKey)) {
            throw new ClientNotFoundException(clientKey);
        }
        repository.deleteByClientKey(clientKey);
        log.info("Deleted client configuration for key={}", clientKey);
    }
}
