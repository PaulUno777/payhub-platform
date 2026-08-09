package com.payhub.orchestrator.infrastructure.persistence;

import java.util.Currency;

import com.payhub.orchestrator.domain.Money;
import com.payhub.orchestrator.domain.Payment;

final class PaymentMapper {

    private PaymentMapper() {
    }

    static Payment toDomain(PaymentJpaEntity entity) {
        return Payment.rehydrate(
                entity.getId(),
                entity.getMerchantId(),
                entity.getTenantId(),
                new Money(entity.getAmount(), Currency.getInstance(entity.getCurrency())),
                entity.getClientReference(),
                entity.getStatus(),
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
        entity.setCreatedAt(payment.createdAt());
        entity.setUpdatedAt(payment.updatedAt());
        return entity;
    }
}
