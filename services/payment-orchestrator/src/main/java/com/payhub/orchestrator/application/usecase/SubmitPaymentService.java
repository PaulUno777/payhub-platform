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
import com.payhub.orchestrator.application.dto.PaymentView;
import com.payhub.orchestrator.application.dto.SubmitPaymentCommand;
import com.payhub.orchestrator.application.port.in.SubmitPaymentUseCase;
import com.payhub.orchestrator.application.port.out.IdempotencyStore;
import com.payhub.orchestrator.application.port.out.PaymentRepository;
import com.payhub.orchestrator.application.port.out.RiskPort;
import com.payhub.orchestrator.application.port.out.RiskPort.RiskCommand;
import com.payhub.orchestrator.application.port.out.RiskPort.RiskDecision;
import com.payhub.orchestrator.application.port.out.WorkflowPort;
import com.payhub.orchestrator.application.port.out.WorkflowPort.PaymentCaptureStart;
import com.payhub.orchestrator.domain.IllegalPaymentStateException;
import com.payhub.orchestrator.domain.Money;
import com.payhub.orchestrator.domain.Payment;

@Service
public class SubmitPaymentService implements SubmitPaymentUseCase {

    static final String OPERATION = "submit-payment";

    private final PaymentRepository paymentRepository;
    private final IdempotencyStore idempotencyStore;
    private final WorkflowPort workflowPort;
    private final RiskPort riskPort;

    public SubmitPaymentService(
            PaymentRepository paymentRepository,
            IdempotencyStore idempotencyStore,
            WorkflowPort workflowPort,
            RiskPort riskPort
    ) {
        this.paymentRepository = paymentRepository;
        this.idempotencyStore = idempotencyStore;
        this.workflowPort = workflowPort;
        this.riskPort = riskPort;
    }

    @Override
    @Transactional
    public PaymentView execute(SubmitPaymentCommand command) {
        Objects.requireNonNull(command.idempotencyKey(), "idempotencyKey");
        String hash = requestHash(command);

        Optional<UUID> existing = idempotencyStore.findPaymentId(OPERATION, command.idempotencyKey());
        if (existing.isPresent()) {
            return PaymentView.from(paymentRepository.findById(existing.get()).orElseThrow());
        }

        Money money = Money.of(command.amount(), command.currencyCode());
        Payment payment = Payment.create(
                command.merchantId(),
                command.tenantId(),
                money,
                command.clientReference()
        );
        Payment saved = paymentRepository.save(payment);
        idempotencyStore.save(OPERATION, command.idempotencyKey(), hash, saved.id());

        workflowPort.startPaymentCapture(new PaymentCaptureStart(
                saved.id(),
                saved.merchantId(),
                saved.tenantId(),
                saved.money().amount().toPlainString(),
                saved.money().currency().getCurrencyCode(),
                saved.clientReference()
        ));

        saved.markRiskPending();
        RiskDecision decision = riskPort.evaluate(new RiskCommand(
                saved.id(),
                saved.merchantId(),
                saved.tenantId(),
                saved.money().amount().toPlainString(),
                saved.money().currency().getCurrencyCode()
        ));

        try {
            switch (decision) {
                case APPROVED -> saved.approveRisk();
                case REJECTED -> saved.rejectRisk();
                case REVIEW -> saved.markRiskReview();
            }
        } catch (IllegalPaymentStateException ex) {
            throw new IllegalPaymentTransitionException(ex.getMessage());
        }

        return PaymentView.from(paymentRepository.save(saved));
    }

    static String requestHash(SubmitPaymentCommand command) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = command.merchantId() + "|" + command.tenantId() + "|"
                    + command.amount() + "|" + command.currencyCode() + "|" + command.clientReference();
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
