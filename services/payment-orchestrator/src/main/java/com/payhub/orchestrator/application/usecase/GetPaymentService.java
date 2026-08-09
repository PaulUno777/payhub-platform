package com.payhub.orchestrator.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.orchestrator.application.PaymentNotFoundException;
import com.payhub.orchestrator.application.dto.PaymentView;
import com.payhub.orchestrator.application.port.in.GetPaymentUseCase;
import com.payhub.orchestrator.application.port.out.PaymentRepository;

@Service
public class GetPaymentService implements GetPaymentUseCase {

    private final PaymentRepository paymentRepository;

    public GetPaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentView execute(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .map(PaymentView::from)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
