package com.payhub.orchestrator.application.usecase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.orchestrator.application.IllegalPaymentTransitionException;
import com.payhub.orchestrator.application.PaymentNotFoundException;
import com.payhub.orchestrator.application.dto.PaymentView;
import com.payhub.orchestrator.application.dto.ResolveReconciliationCommand;
import com.payhub.orchestrator.application.port.in.ResolveReconciliationUseCase;
import com.payhub.orchestrator.application.port.out.IdempotencyStore;
import com.payhub.orchestrator.application.port.out.PaymentRepository;
import com.payhub.orchestrator.domain.IllegalPaymentStateException;
import com.payhub.orchestrator.domain.Payment;

@Service
public class ResolveReconciliationService implements ResolveReconciliationUseCase {

    static final String OPERATION = "resolve-reconciliation";
    static final String ACTION_REQUEST_REVERSAL = "REQUEST_REVERSAL";

    private final PaymentRepository paymentRepository;
    private final IdempotencyStore idempotencyStore;

    public ResolveReconciliationService(
            PaymentRepository paymentRepository,
            IdempotencyStore idempotencyStore
    ) {
        this.paymentRepository = paymentRepository;
        this.idempotencyStore = idempotencyStore;
    }

    @Override
    @Transactional
    public PaymentView execute(ResolveReconciliationCommand command) {
        Objects.requireNonNull(command.paymentId(), "paymentId");
        Objects.requireNonNull(command.action(), "action");
        Objects.requireNonNull(command.idempotencyKey(), "idempotencyKey");

        Optional<UUID> existing = idempotencyStore.findPaymentId(OPERATION, command.idempotencyKey());
        if (existing.isPresent()) {
            return PaymentView.from(paymentRepository.findById(existing.get()).orElseThrow());
        }

        Payment payment = paymentRepository.findById(command.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));

        String action = command.action().trim().toUpperCase();
        if (!ACTION_REQUEST_REVERSAL.equals(action)) {
            throw new IllegalPaymentTransitionException("Unsupported reconciliation action: " + command.action());
        }

        try {
            payment.resolveReconciliationFailed();
        } catch (IllegalPaymentStateException ex) {
            throw new IllegalPaymentTransitionException(ex.getMessage());
        }

        paymentRepository.save(payment);
        idempotencyStore.save(OPERATION, command.idempotencyKey(), requestHash(command), payment.id());
        return PaymentView.from(payment);
    }

    static String requestHash(ResolveReconciliationCommand command) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = command.paymentId() + "|" + command.action();
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
