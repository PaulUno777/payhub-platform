package com.payhub.orchestrator.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.payhub.orchestrator.TestcontainersConfiguration;
import com.payhub.orchestrator.application.port.out.WorkflowPort;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, SubmitPaymentWebIT.WorkflowProbeConfig.class})
class SubmitPaymentWebIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkflowStartProbe workflowStartProbe;

    @Test
    void should_submit_payment_to_risk_approved_idempotently_and_start_workflow_once() throws Exception {
        UUID merchantId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String body = """
                {
                  "merchantId": "%s",
                  "tenantId": "%s",
                  "amount": "25.00",
                  "currencyCode": "USD",
                  "clientReference": "ord-web-1"
                }
                """.formatted(merchantId, tenantId);

        MvcResult first = mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "idem-web-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RISK_APPROVED"))
                .andReturn();

        JsonNode payment = objectMapper.readTree(first.getResponse().getContentAsString());
        String paymentId = payment.get("id").asString();

        mockMvc.perform(post("/api/v1/payments")
                        .header("Idempotency-Key", "idem-web-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.status").value("RISK_APPROVED"));

        mockMvc.perform(get("/api/v1/payments/" + paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RISK_APPROVED"));

        assertThat(workflowStartProbe.starts()).isEqualTo(1);
    }

    @TestConfiguration
    static class WorkflowProbeConfig {

        @Bean
        WorkflowStartProbe workflowStartProbe() {
            return new WorkflowStartProbe();
        }

        @Bean
        @Primary
        WorkflowPort probingWorkflowPort(WorkflowStartProbe probe) {
            return new WorkflowPort() {
                @Override
                public void startPaymentCapture(PaymentCaptureStart command) {
                    probe.increment();
                }

                @Override
                public void signalContinueCapture(java.util.UUID paymentId) {
                    // no-op in web IT; Temporal disabled
                }

                @Override
                public void startRefund(RefundStart command) {
                    // no-op
                }
            };
        }
    }

    static final class WorkflowStartProbe {
        private final AtomicInteger starts = new AtomicInteger();

        void increment() {
            starts.incrementAndGet();
        }

        int starts() {
            return starts.get();
        }
    }
}
