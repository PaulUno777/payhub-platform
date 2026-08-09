package com.payhub.orchestrator.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payhub.orchestrator.application.dto.PaymentView;
import com.payhub.orchestrator.application.dto.SubmitPaymentCommand;
import com.payhub.orchestrator.application.port.out.IdempotencyStore;
import com.payhub.orchestrator.application.port.out.PaymentLifecyclePublisher;
import com.payhub.orchestrator.application.port.out.PaymentRepository;
import com.payhub.orchestrator.application.port.out.RiskPort;
import com.payhub.orchestrator.application.port.out.RiskPort.RiskDecision;
import com.payhub.orchestrator.application.port.out.WorkflowPort;
import com.payhub.orchestrator.domain.Payment;
import com.payhub.orchestrator.domain.PaymentStatus;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class SubmitPaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private IdempotencyStore idempotencyStore;
    @Mock
    private WorkflowPort workflowPort;
    @Mock
    private RiskPort riskPort;
    @Mock
    private PaymentLifecyclePublisher paymentLifecyclePublisher;

    private SubmitPaymentService service;
    private final Map<UUID, Payment> store = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new SubmitPaymentService(
                paymentRepository,
                idempotencyStore,
                workflowPort,
                riskPort,
                paymentLifecyclePublisher
        );
        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            store.put(p.id(), p);
            return p;
        });
        lenient().when(paymentRepository.findById(any()))
                .thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0))));
        when(idempotencyStore.findPaymentId(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void should_reach_risk_approved_and_start_workflow() {
        when(riskPort.evaluate(any())).thenReturn(RiskDecision.APPROVED);
        SubmitPaymentCommand command = command("key-1");

        PaymentView view = service.execute(command);

        assertThat(view.status()).isEqualTo(PaymentStatus.RISK_APPROVED.name());
        verify(workflowPort, times(1)).startPaymentCapture(any());
        verify(idempotencyStore).save(any(), any(), any(), any());
        verify(paymentLifecyclePublisher).publishStatusChanged(any(), any(), any(), eq(PaymentStatus.RISK_APPROVED));
    }

    @Test
    void should_reject_without_rail_and_still_start_workflow() {
        when(riskPort.evaluate(any())).thenReturn(RiskDecision.REJECTED);

        PaymentView view = service.execute(command("key-2"));

        assertThat(view.status()).isEqualTo(PaymentStatus.RISK_REJECTED.name());
        verify(workflowPort, times(1)).startPaymentCapture(any());
    }

    @Test
    void should_replay_idempotent_submit_without_second_workflow_start() {
        when(riskPort.evaluate(any())).thenReturn(RiskDecision.APPROVED);
        PaymentView first = service.execute(command("key-3"));
        when(idempotencyStore.findPaymentId(SubmitPaymentService.OPERATION, "key-3"))
                .thenReturn(Optional.of(first.id()));

        PaymentView second = service.execute(command("key-3"));

        assertThat(second.id()).isEqualTo(first.id());
        verify(workflowPort, times(1)).startPaymentCapture(any());
        verify(riskPort, times(1)).evaluate(any());
    }

    private static SubmitPaymentCommand command(String key) {
        return new SubmitPaymentCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "25.00",
                "USD",
                "ord-1",
                key
        );
    }
}
