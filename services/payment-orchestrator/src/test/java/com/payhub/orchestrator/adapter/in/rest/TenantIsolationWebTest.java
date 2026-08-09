package com.payhub.orchestrator.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.payhub.orchestrator.TestJwtDecoderConfig;
import com.payhub.orchestrator.TestcontainersConfiguration;
import com.payhub.orchestrator.infrastructure.security.TenantIsolationFilter;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TenantIsolationWebTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void should_reject_when_tenant_header_mismatches_jwt_claim() throws Exception {
        mockMvc.perform(get("/api/v1/payments/" + UUID.randomUUID())
                        .header("Authorization", "Bearer test-token")
                        .header(TenantIsolationFilter.TENANT_HEADER, UUID.randomUUID().toString()))
                .andExpect(status().isForbidden())
                .andExpect(content().string("TENANT_CLAIM_MISMATCH"));
    }

    @Test
    void should_allow_matching_tenant_header() throws Exception {
        mockMvc.perform(get("/api/v1/payments/" + UUID.randomUUID())
                        .header("Authorization", "Bearer test-token")
                        .header(TenantIsolationFilter.TENANT_HEADER, TestJwtDecoderConfig.TENANT_ID))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 403) {
                        throw new AssertionError("Unexpected TENANT_CLAIM_MISMATCH");
                    }
                });
    }
}
