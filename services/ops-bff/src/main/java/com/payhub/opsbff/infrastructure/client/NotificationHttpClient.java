package com.payhub.opsbff.infrastructure.client;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.payhub.opsbff.application.port.out.NotificationPort;
import com.payhub.opsbff.infrastructure.security.TenantIsolationFilter;

@Component
public class NotificationHttpClient implements NotificationPort {

    private static final ParameterizedTypeReference<List<WebhookDeliveryDto>> DLQ_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    public NotificationHttpClient(
            RestClient.Builder restClientBuilder,
            MerchantServiceProperties properties
    ) {
        this.restClient = restClientBuilder
                .baseUrl(Objects.requireNonNull(
                        properties.notificationService(),
                        "payhub.clients.notification-service"
                ))
                .build();
    }

    @Override
    public List<WebhookDeliveryDto> listDeadLetterQueue(UUID tenantId, int limit) {
        List<WebhookDeliveryDto> body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/webhooks/dlq")
                        .queryParam("limit", limit)
                        .build())
                .header(TenantIsolationFilter.TENANT_HEADER, tenantId.toString())
                .retrieve()
                .body(DLQ_TYPE);
        return body == null ? List.of() : body;
    }
}
