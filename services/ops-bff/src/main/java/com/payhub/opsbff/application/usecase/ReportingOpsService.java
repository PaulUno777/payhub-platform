package com.payhub.opsbff.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.payhub.opsbff.application.port.in.ReportingOpsUseCase;
import com.payhub.opsbff.application.port.out.ReportingPort;
import com.payhub.opsbff.application.port.out.ReportingPort.PaymentLifecycleViewDto;

@Service
public class ReportingOpsService implements ReportingOpsUseCase {

    private final ReportingPort reportingPort;

    public ReportingOpsService(ReportingPort reportingPort) {
        this.reportingPort = reportingPort;
    }

    @Override
    public PaymentLifecycleViewDto getPaymentLifecycleView(UUID tenantId, UUID paymentId) {
        return reportingPort.getPaymentLifecycleView(tenantId, paymentId);
    }
}
