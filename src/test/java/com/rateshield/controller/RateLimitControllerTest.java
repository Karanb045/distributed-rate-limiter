package com.rateshield.controller;

import com.rateshield.dto.RateLimitResponse;
import com.rateshield.enums.RateLimitAlgorithm;
import com.rateshield.enums.RateLimitStatus;
import com.rateshield.exception.ClientNotFoundException;
import com.rateshield.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RateLimitController.class)
@AutoConfigureMockMvc(addFilters = false)
class RateLimitControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    RateLimitService service;

    @Test
    void allowReturns200() throws Exception {
        RateLimitResponse resp = RateLimitResponse.builder()
                .status(RateLimitStatus.ALLOW).algorithm(RateLimitAlgorithm.TOKEN_BUCKET)
                .remainingTokens(9).limit(10).resetAfterMillis(0L).timestamp(LocalDateTime.now()).build();
        when(service.check("c1")).thenReturn(resp);

        mockMvc.perform(post("/api/v1/rate-limit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientKey\":\"c1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALLOW"));
    }

    @Test
    void denyReturns429WithRetryAfter() throws Exception {
        RateLimitResponse resp = RateLimitResponse.builder()
                .status(RateLimitStatus.DENY).algorithm(RateLimitAlgorithm.TOKEN_BUCKET)
                .remainingTokens(0).limit(10).resetAfterMillis(500L).timestamp(LocalDateTime.now()).build();
        when(service.check("c1")).thenReturn(resp);

        mockMvc.perform(post("/api/v1/rate-limit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientKey\":\"c1\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "1"))
                .andExpect(jsonPath("$.status").value("DENY"));
    }

    @Test
    void missingClientReturns404() throws Exception {
        when(service.check("missing")).thenThrow(new ClientNotFoundException("missing"));
        mockMvc.perform(post("/api/v1/rate-limit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientKey\":\"missing\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void blankClientKeyReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/rate-limit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientKey\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
