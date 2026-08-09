package com.payhub.orchestrator.application.usecase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.orchestrator.application.dto.PaymentView;
import com.payhub.orchestrator.application.dto.RequestRefundCommand;
import com.payhub.orchestrator.application.port.in.RequestRefundUseCase;
import com.payhub.orchestrator.application.port.out.IdempotencyStore;
import com.payhub.orchestrator.application.port.out.PaymentRepository;
import com.payhub.orchestrator.application.port.out.WorkflowPort;
import com.payhub.orchestrator.application.port.out.WorkflowPort.RefundStart;
import com.payhub.orchestrator.domain.Payment;

@Service
public class RequestRefundService implements RequestRefundUseCase {

    static final String OPERATION = "request-refund";

    private final PaymentRepository paymentRepository;
    private final IdempotencyStore idempotencyStore;
    private final WorkflowPort workflowPort;
    private final String defaultSandboxMode;

    public RequestRefundService(
            PaymentRepository paymentRepository,
            IdempotencyStore idempotencyStore,
            WorkflowPort workflowPort,
            @Value("${payhub.rail.sandbox-mode:ACCEPT}") String defaultSandboxMode
    ) {
        this.paymentRepository = paymentRepository;
        this.idempotencyStore = idempotencyStore;
        this.workflowPort = workflowPort;
        this.defaultSandboxMode = defaultSandboxMode;
    }

    @Override
    @Transactional
    public PaymentView execute(RequestRefundCommand command) {
        Objects.requireNonNull(command.idempotencyKey(), "idempotencyKey");
        Objects.requireNonNull(command.paymentId(), "paymentId");
        Objects.requireNonNull(command.refundAmount(), "refundAmount");

        Optional<UUID> existing = idempotencyStore.findPaymentId(OPERATION, command.idempotencyKey());
        if (existing.isPresent()) {
            return PaymentView.from(paymentRepository.findById(existing.get()).orElseThrow());
        }

        Payment payment = paymentRepository.findById(command.paymentId())
                .orElseThrow(() -> new com.payhub.orchestrator.application.PaymentNotFoundException(command.paymentId()));
        String sandboxMode = command.sandboxMode() == null || command.sandboxMode().isBlank()
                ? defaultSandboxMode
                : command.sandboxMode();

        String hash = requestHash(command);
        idempotencyStore.save(OPERATION, command.idempotencyKey(), hash, payment.id());

        workflowPort.startRefund(new RefundStart(
                payment.id(),
                command.refundAmount(),
                sandboxMode
        ));

        return PaymentView.from(payment);
    }

    static String requestHash(RequestRefundCommand command) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = command.paymentId() + "|" + command.refundAmount();
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
