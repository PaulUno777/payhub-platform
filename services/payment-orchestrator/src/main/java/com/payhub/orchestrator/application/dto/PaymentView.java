package com.payhub.orchestrator.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.payhub.orchestrator.domain.Payment;

public record PaymentView(
        UUID id,
        UUID merchantId,
        UUID tenantId,
        String amount,
        String currency,
        String clientReference,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static PaymentView from(Payment payment) {
        return new PaymentView(
                payment.id(),
                payment.merchantId(),
                payment.tenantId(),
                payment.money().amount().toPlainString(),
                payment.money().currency().getCurrencyCode(),
                payment.clientReference(),
                payment.status().name(),
                payment.createdAt(),
                payment.updatedAt()
        );
    }
}
