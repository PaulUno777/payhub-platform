package com.payhub.orchestrator.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.Currency;

import com.payhub.orchestrator.domain.Money;
import com.payhub.orchestrator.domain.Payment;

final class PaymentMapper {

    private PaymentMapper() {
    }

    static Payment toDomain(PaymentJpaEntity entity) {
        BigDecimal refunded = entity.getRefundedAmount() == null ? BigDecimal.ZERO : entity.getRefundedAmount();
        return Payment.rehydrate(
                entity.getId(),
                entity.getMerchantId(),
                entity.getTenantId(),
                new Money(entity.getAmount(), Currency.getInstance(entity.getCurrency())),
                entity.getClientReference(),
                entity.getStatus(),
                entity.getRailReference(),
                entity.getInitiateJournalEntryId(),
                refunded,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    static PaymentJpaEntity toEntity(Payment payment) {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setId(payment.id());
        entity.setMerchantId(payment.merchantId());
        entity.setTenantId(payment.tenantId());
        entity.setAmount(payment.money().amount());
        entity.setCurrency(payment.money().currency().getCurrencyCode());
        entity.setClientReference(payment.clientReference());
        entity.setStatus(payment.status());
        entity.setRailReference(payment.railReference());
        entity.setInitiateJournalEntryId(payment.initiateJournalEntryId());
        entity.setRefundedAmount(payment.refundedAmount());
        entity.setCreatedAt(payment.createdAt());
        entity.setUpdatedAt(payment.updatedAt());
        return entity;
    }
}
