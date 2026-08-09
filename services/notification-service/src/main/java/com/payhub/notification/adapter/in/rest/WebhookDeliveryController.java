package com.payhub.notification.adapter.in.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.payhub.notification.application.dto.WebhookDeliveryView;
import com.payhub.notification.application.port.in.GetWebhookDeliveryUseCase;
import com.payhub.notification.application.port.in.ListDeadLetterWebhooksUseCase;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookDeliveryController {

    public static final String TENANT_HEADER = "X-PayHub-Tenant-Id";

    private final ListDeadLetterWebhooksUseCase listDeadLetterWebhooksUseCase;
    private final GetWebhookDeliveryUseCase getWebhookDeliveryUseCase;

    public WebhookDeliveryController(
            ListDeadLetterWebhooksUseCase listDeadLetterWebhooksUseCase,
            GetWebhookDeliveryUseCase getWebhookDeliveryUseCase
    ) {
        this.listDeadLetterWebhooksUseCase = listDeadLetterWebhooksUseCase;
        this.getWebhookDeliveryUseCase = getWebhookDeliveryUseCase;
    }

    @GetMapping("/dlq")
    public List<WebhookDeliveryView> listDlq(
            @RequestHeader(TENANT_HEADER) UUID tenantId,
            @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        return listDeadLetterWebhooksUseCase.execute(tenantId, limit);
    }

    @GetMapping("/deliveries/{deliveryId}")
    public WebhookDeliveryView getDelivery(
            @PathVariable UUID deliveryId,
            @RequestHeader(TENANT_HEADER) UUID tenantId
    ) {
        return getWebhookDeliveryUseCase.execute(tenantId, deliveryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Webhook delivery not found"));
    }
}
