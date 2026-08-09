package com.payhub.reporting.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.payhub.reporting.application.dto.PaymentLifecycleView;
import com.payhub.reporting.application.dto.PaymentLifecycleView.Freshness;
import com.payhub.reporting.application.port.in.GetPaymentLifecycleViewUseCase;

@Tag("unit")
@WebMvcTest(controllers = PaymentLifecycleViewController.class)
class PaymentLifecycleViewControllerTest {

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private GetPaymentLifecycleViewUseCase getPaymentLifecycleViewUseCase;

    @Test
    void should_return_view_with_freshness_metadata() {
        UUID paymentId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        PaymentLifecycleView view = new PaymentLifecycleView(
                paymentId,
                tenantId,
                UUID.randomUUID(),
                "SETTLED",
                Instant.parse("2026-08-09T11:00:00Z"),
                Instant.parse("2026-08-09T11:01:00Z"),
                5_000L,
                Freshness.NON_AUTHORITATIVE
        );
        when(getPaymentLifecycleViewUseCase.execute(tenantId, paymentId)).thenReturn(Optional.of(view));

        assertThat(mockMvc.get()
                .uri("/api/v1/reporting/payments/{id}", paymentId)
                .header(PaymentLifecycleViewController.TENANT_HEADER, tenantId.toString())
        ).hasStatusOk()
                .bodyJson()
                .extractingPath("$.freshness").asString().isEqualTo("NON_AUTHORITATIVE");

        verify(getPaymentLifecycleViewUseCase).execute(eq(tenantId), eq(paymentId));
    }

    @Test
    void should_return_404_when_missing() {
        UUID paymentId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        when(getPaymentLifecycleViewUseCase.execute(tenantId, paymentId)).thenReturn(Optional.empty());

        assertThat(mockMvc.get()
                .uri("/api/v1/reporting/payments/{id}", paymentId)
                .header(PaymentLifecycleViewController.TENANT_HEADER, tenantId.toString())
        ).hasStatus(404);
    }
}
