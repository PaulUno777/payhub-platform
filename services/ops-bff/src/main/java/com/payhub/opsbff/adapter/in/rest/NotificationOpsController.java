package com.payhub.opsbff.adapter.in.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.payhub.opsbff.application.port.in.NotificationOpsUseCase;
import com.payhub.opsbff.application.port.out.NotificationPort.WebhookDeliveryDto;
import com.payhub.opsbff.infrastructure.security.TenantIsolationFilter;

@RestController
@RequestMapping("/ops/notifications")
public class NotificationOpsController {

    private final NotificationOpsUseCase notificationOpsUseCase;

    public NotificationOpsController(NotificationOpsUseCase notificationOpsUseCase) {
        this.notificationOpsUseCase = notificationOpsUseCase;
    }

    @GetMapping("/dlq")
    public List<WebhookDeliveryDto> listDlq(
            @RequestHeader(TenantIsolationFilter.TENANT_HEADER) UUID tenantId,
            @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        return notificationOpsUseCase.listDeadLetterQueue(tenantId, limit);
    }
}
