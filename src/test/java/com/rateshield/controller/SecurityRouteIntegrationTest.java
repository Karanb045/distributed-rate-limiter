package com.rateshield.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the real SecurityFilterChain (HTTP Basic + role-based rules) against a
 * live Spring context. Runs in a rolled-back transaction so the seeded client
 * never persists.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityRouteIntegrationTest {

    private static final String ADMIN = "admin";
    private static final String ADMIN_PASS = "change-me-admin";
    private static final String OPS = "ops";
    private static final String OPS_PASS = "change-me-ops";

    @Autowired
    MockMvc mockMvc;

    @BeforeEach
    void seedClient() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .header(HttpHeaders.AUTHORIZATION, basic(ADMIN, ADMIN_PASS))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientKey":"sec-test","algorithm":"TOKEN_BUCKET",\
                                "requestsPerSecond":100,"burstCapacity":100}"""))
                .andExpect(status().isCreated());
    }

    @Test
    void anonymousAccessToClientConfigIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousAccessToRateLimitCheckIsAllowed() throws Exception {
        // Public endpoint: no auth required.
        mockMvc.perform(post("/api/v1/rate-limit/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientKey\":\"sec-test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void infoEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void basicAuthSucceedsForValidCredentials() throws Exception {
        mockMvc.perform(get("/api/v1/clients")
                        .header(HttpHeaders.AUTHORIZATION, basic(ADMIN, ADMIN_PASS)))
                .andExpect(status().isOk());
    }

    @Test
    void basicAuthRejectsBadCredentials() throws Exception {
        mockMvc.perform(get("/api/v1/clients")
                        .header(HttpHeaders.AUTHORIZATION, basic(ADMIN, "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void metricsEndpointRequiresOpsRole() throws Exception {
        // Anonymous: rejected.
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());

        // ADMIN: authenticated but lacks OPS -> forbidden.
        mockMvc.perform(get("/actuator/metrics")
                        .header(HttpHeaders.AUTHORIZATION, basic(ADMIN, ADMIN_PASS)))
                .andExpect(status().isForbidden());

        // OPS: allowed.
        mockMvc.perform(get("/actuator/metrics")
                        .header(HttpHeaders.AUTHORIZATION, basic(OPS, OPS_PASS)))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanAccessClientConfig() throws Exception {
        mockMvc.perform(get("/api/v1/clients")
                        .header(HttpHeaders.AUTHORIZATION, basic(ADMIN, ADMIN_PASS)))
                .andExpect(status().isOk());
    }

    @Test
    void opsCannotAccessClientConfig() throws Exception {
        mockMvc.perform(get("/api/v1/clients")
                        .header(HttpHeaders.AUTHORIZATION, basic(OPS, OPS_PASS)))
                .andExpect(status().isForbidden());
    }

    private String basic(String user, String pass) {
        return "Basic " + Base64.getEncoder()
                .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
    }
}
