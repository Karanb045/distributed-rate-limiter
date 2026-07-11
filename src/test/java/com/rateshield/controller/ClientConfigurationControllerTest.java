package com.rateshield.controller;
import com.rateshield.dto.ClientConfigResponse;
import com.rateshield.enums.RateLimitAlgorithm;
import com.rateshield.exception.ClientNotFoundException;
import com.rateshield.exception.DuplicateClientException;
import com.rateshield.service.ClientConfigurationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientConfigurationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClientConfigurationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ClientConfigurationService service;

    private static final String VALID_BODY =
            "{\"clientKey\":\"c1\",\"algorithm\":\"TOKEN_BUCKET\",\"requestsPerSecond\":10,\"burstCapacity\":10}";

    @Test
    void createReturns201() throws Exception {
        ClientConfigResponse resp = ClientConfigResponse.builder()
                .clientKey("c1").algorithm(RateLimitAlgorithm.TOKEN_BUCKET)
                .requestsPerSecond(BigDecimal.TEN).burstCapacity(10).build();
        when(service.create(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientKey").value("c1"));
    }

    @Test
    void createValidatesInput() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientKey\":\"\",\"algorithm\":null,\"requestsPerSecond\":null,\"burstCapacity\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMissingReturns404() throws Exception {
        when(service.getByKey("missing")).thenThrow(new ClientNotFoundException("missing"));
        mockMvc.perform(get("/api/v1/clients/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createDuplicateReturns409() throws Exception {
        when(service.create(any())).thenThrow(new DuplicateClientException("c1"));
        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/c1"))
                .andExpect(status().isNoContent());
    }
}
