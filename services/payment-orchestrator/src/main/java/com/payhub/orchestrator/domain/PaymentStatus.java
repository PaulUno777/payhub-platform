package com.payhub.orchestrator.domain;

public enum PaymentStatus {
    CREATED,
    RISK_PENDING,
    RISK_APPROVED,
    RISK_REJECTED,
    RISK_REVIEW,
    // Placeholders for DS-008+
    RAIL_SUBMITTED,
    SETTLEMENT_PENDING,
    SETTLED,
    FAILED_FINAL,
    RECONCILIATION_REQUIRED
}
