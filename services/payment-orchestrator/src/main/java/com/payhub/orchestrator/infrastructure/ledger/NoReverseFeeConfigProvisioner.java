package com.payhub.orchestrator.infrastructure.ledger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.payhub.orchestrator.application.port.out.LedgerPort;
import com.payhub.orchestrator.application.port.out.LedgerPort.PutFeeConfigCommand;

/**
 * Provisions EcoPay v1 feeReversalPolicy=NO_REVERSE on the sandbox tenant (DS-009).
 */
@Component
@ConditionalOnProperty(prefix = "payhub.finledger", name = "provision-fee-config", havingValue = "true")
public class NoReverseFeeConfigProvisioner {

    private static final Logger log = LoggerFactory.getLogger(NoReverseFeeConfigProvisioner.class);

    private final LedgerPort ledgerPort;
    private final FinLedgerProperties properties;

    public NoReverseFeeConfigProvisioner(LedgerPort ledgerPort, FinLedgerProperties properties) {
        this.ledgerPort = ledgerPort;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void provision() {
        ledgerPort.putFeeConfig(new PutFeeConfigCommand(
                properties.feeConfigTenantId(),
                "NO_REVERSE",
                properties.bearerToken()
        ));
        log.info("Provisioned FinLedger fee-config NO_REVERSE for tenant {}", properties.feeConfigTenantId());
    }
}
