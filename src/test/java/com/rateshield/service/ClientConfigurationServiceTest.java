package com.rateshield.service;

import com.rateshield.dto.ClientConfigRequest;
import com.rateshield.entity.ClientConfiguration;
import com.rateshield.enums.RateLimitAlgorithm;
import com.rateshield.exception.ClientNotFoundException;
import com.rateshield.exception.DuplicateClientException;
import com.rateshield.mapper.ClientConfigurationMapper;
import com.rateshield.repository.ClientConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientConfigurationServiceTest {

    @Mock
    ClientConfigurationRepository repository;

    @Mock
    ClientConfigurationMapper mapper;

    @InjectMocks
    ClientConfigurationService service;

    @Test
    void createTranslatesUniqueConstraintViolationToDuplicateClient() {
        ClientConfigRequest request = new ClientConfigRequest("client-1", RateLimitAlgorithm.TOKEN_BUCKET, BigDecimal.TEN, 10);
        when(repository.existsByClientKey("client-1")).thenReturn(false);
        when(mapper.toEntity(any())).thenReturn(ClientConfiguration.builder().clientKey("client-1").build());
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        DuplicateClientException exception = assertThrows(DuplicateClientException.class, () -> service.create(request));

        assertEquals("client-1", exception.getClientKey());
    }

    @Test
    void getByKeyThrowsWhenMissing() {
        when(repository.findByClientKey("missing")).thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class, () -> service.getByKey("missing"));
    }
}