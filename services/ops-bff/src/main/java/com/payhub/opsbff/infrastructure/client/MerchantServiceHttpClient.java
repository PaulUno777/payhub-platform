package com.payhub.opsbff.infrastructure.client;

import java.util.Objects;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.payhub.opsbff.application.port.out.MerchantServicePort;

@Component
public class MerchantServiceHttpClient implements MerchantServicePort {

    private final RestClient restClient;

    public MerchantServiceHttpClient(
            RestClient.Builder restClientBuilder,
            MerchantServiceProperties properties
    ) {
        this.restClient = restClientBuilder
                .baseUrl(Objects.requireNonNull(properties.merchantService(), "payhub.clients.merchant-service"))
                .build();
    }

    @Override
    public MerchantDto get(UUID merchantId) {
        return restClient.get()
                .uri("/api/v1/merchants/{id}", merchantId)
                .retrieve()
                .body(MerchantDto.class);
    }

    @Override
    public MerchantDto approve(UUID merchantId, String idempotencyKey, String authorizationHeader) {
        var spec = restClient.post()
                .uri("/api/v1/merchants/{id}/approve", merchantId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey);
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            spec = spec.header("Authorization", authorizationHeader);
        }
        return spec.retrieve().body(MerchantDto.class);
    }

    @Override
    public MerchantDto reject(UUID merchantId, String reason, String idempotencyKey) {
        return restClient.post()
                .uri("/api/v1/merchants/{id}/reject", merchantId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(new RejectBody(reason))
                .retrieve()
                .body(MerchantDto.class);
    }

    private record RejectBody(String reason) {
    }
}
