package com.rateshield.controller;

import com.rateshield.dto.ClientConfigRequest;
import com.rateshield.dto.ClientConfigResponse;
import com.rateshield.service.ClientConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for managing client rate limit configurations.
 */
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Client Configuration", description = "Manage client rate limit configurations")
public class ClientConfigurationController {

    private final ClientConfigurationService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a client rate limit configuration")
    public ClientConfigResponse create(@Valid @RequestBody ClientConfigRequest request) {
        return service.create(request);
    }

    @GetMapping("/{clientKey}")
    @Operation(summary = "Get a client configuration by its key")
    public ClientConfigResponse getByKey(@PathVariable String clientKey) {
        return service.getByKey(clientKey);
    }

    @GetMapping
    @Operation(summary = "List all client configurations")
    public List<ClientConfigResponse> getAll() {
        return service.getAll();
    }

    @PutMapping("/{clientKey}")
    @Operation(summary = "Update an existing client configuration")
    public ClientConfigResponse update(@PathVariable String clientKey,
                                       @Valid @RequestBody ClientConfigRequest request) {
        return service.update(clientKey, request);
    }

    @DeleteMapping("/{clientKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a client configuration")
    public void delete(@PathVariable String clientKey) {
        service.delete(clientKey);
    }
}
