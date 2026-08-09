package com.payhub.gateway.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.payhub.gateway.TestJwtDecoderConfig;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwtDecoderConfig.class)
class GatewaySecurityWebTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void should_reject_request_without_jwt_before_business_route() throws Exception {
        mockMvc.perform(get("/ops/reconciliation/breaks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_allow_actuator_health_without_jwt() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void should_authenticate_bearer_jwt_before_routing() throws Exception {
        // No matching gateway route → 404 after JWT auth (must not be 401).
        mockMvc.perform(get("/__secure-probe")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNotFound());
    }
}