package com.payhub.reconciliation.application.port.out;

import java.util.List;
import java.util.UUID;

public interface StatementSourcePort {

    List<StatementLine> load(String statementKey);

    record StatementLine(
            String externalRef,
            UUID paymentId,
            String statementStatus,
            String amount,
            String currencyCode
    ) {
    }
}
