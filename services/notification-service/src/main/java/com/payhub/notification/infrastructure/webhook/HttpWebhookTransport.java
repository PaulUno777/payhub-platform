package com.payhub.notification.infrastructure.webhook;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.payhub.notification.application.port.out.WebhookTransportPort;
import com.payhub.notification.infrastructure.config.NotificationWebhookProperties;

import tools.jackson.databind.ObjectMapper;

@Component
public class HttpWebhookTransport implements WebhookTransportPort {

    private static final Logger log = LoggerFactory.getLogger(HttpWebhookTransport.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final NotificationWebhookProperties properties;

    public HttpWebhookTransport(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            NotificationWebhookProperties properties
    ) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public DeliveryResult deliver(WebhookPayload payload, String endpointUrl) {
        if (endpointUrl == null || endpointUrl.isBlank()) {
            return DeliveryResult.failed(0, "webhook endpoint not configured");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("eventId", payload.eventId().toString());
            body.put("paymentId", payload.paymentId().toString());
            body.put("tenantId", payload.tenantId().toString());
            body.put("status", payload.status());
            body.put("occurredAt", payload.occurredAt().toString());

            byte[] rawBody = objectMapper.writeValueAsBytes(body);
            String signature = HmacSignatures.sha256HeaderValue(properties.hmacSecret(), rawBody);

            restClient.post()
                    .uri(endpointUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HmacSignatures.HEADER, signature)
                    .body(rawBody)
                    .retrieve()
                    .toBodilessEntity();

            return DeliveryResult.ok(200);
        } catch (RestClientResponseException ex) {
            log.debug("Webhook HTTP failure status={} url={}", ex.getStatusCode().value(), endpointUrl);
            return DeliveryResult.failed(ex.getStatusCode().value(), "HTTP " + ex.getStatusCode().value());
        } catch (Exception ex) {
            log.debug("Webhook transport failure url={}: {}", endpointUrl, ex.toString());
            return DeliveryResult.failed(0, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        }
    }
}
