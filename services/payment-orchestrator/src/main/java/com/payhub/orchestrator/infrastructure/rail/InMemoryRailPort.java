package com.payhub.orchestrator.infrastructure.rail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.payhub.orchestrator.application.port.out.RailPort;

/**
 * In-process MmSandbox stand-in when rail-adapter is not running (tests / local).
 */
@Component
@ConditionalOnProperty(prefix = "payhub.rail", name = "mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryRailPort implements RailPort {

    private final String defaultMode;

    public InMemoryRailPort(
            @Value("${payhub.rail.sandbox-mode:ACCEPT}") String defaultMode
    ) {
        this.defaultMode = defaultMode;
    }

    @Override
    public RailSubmitResult submit(RailSubmitCommand command) {
        return resolveSubmit(command, "mm-");
    }

    @Override
    public RailProofResult awaitFinalProof(RailProofCommand command) {
        return resolveProof(command);
    }

    @Override
    public RailSubmitResult submitRefund(RailSubmitCommand command) {
        return resolveSubmit(command, "mm-refund-");
    }

    @Override
    public RailProofResult awaitRefundProof(RailProofCommand command) {
        return resolveProof(command);
    }

    private RailSubmitResult resolveSubmit(RailSubmitCommand command, String prefix) {
        String mode = command.sandboxMode() == null || command.sandboxMode().isBlank()
                ? defaultMode
                : command.sandboxMode();
        return switch (mode.toUpperCase()) {
            case "REJECT" -> new RailSubmitResult(RailResult.REJECTED, null);
            case "AMBIGUOUS" -> new RailSubmitResult(RailResult.AMBIGUOUS, null);
            case "ACCEPT", "PROOF_AMBIGUOUS" -> new RailSubmitResult(
                    RailResult.ACCEPTED,
                    prefix + command.paymentId()
            );
            default -> throw new IllegalArgumentException("Unknown sandbox mode: " + mode);
        };
    }

    private RailProofResult resolveProof(RailProofCommand command) {
        String mode = command.sandboxMode() == null || command.sandboxMode().isBlank()
                ? defaultMode
                : command.sandboxMode();
        if ("PROOF_AMBIGUOUS".equalsIgnoreCase(mode)) {
            return new RailProofResult(RailResult.AMBIGUOUS);
        }
        if (command.providerReference() == null || command.providerReference().isBlank()) {
            return new RailProofResult(RailResult.AMBIGUOUS);
        }
        return new RailProofResult(RailResult.ACCEPTED);
    }
}
