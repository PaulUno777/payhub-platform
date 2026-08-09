package com.payhub.opsbff.infrastructure.client;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.payhub.opsbff.application.port.out.ReconciliationPort;

@Component
public class ReconciliationHttpClient implements ReconciliationPort {

    private final RestClient restClient;

    public ReconciliationHttpClient(
            RestClient.Builder restClientBuilder,
            MerchantServiceProperties properties
    ) {
        this.restClient = restClientBuilder
                .baseUrl(Objects.requireNonNull(
                        properties.reconciliationService(),
                        "payhub.clients.reconciliation-service"
                ))
                .build();
    }

    @Override
    public RunDto startRun(UUID tenantId, String railCode, String statementKey) {
        return restClient.post()
                .uri("/api/v1/reconciliation/runs")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new StartRunBody(tenantId, railCode, statementKey))
                .retrieve()
                .body(RunDto.class);
    }

    @Override
    public List<BreakDto> listBreaks(UUID runId) {
        BreakDto[] body = restClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder.path("/api/v1/reconciliation/breaks");
                    if (runId != null) {
                        b.queryParam("runId", runId);
                    }
                    return b.build();
                })
                .retrieve()
                .body(BreakDto[].class);
        return body == null ? List.of() : Arrays.asList(body);
    }

    @Override
    public BreakDto getBreak(UUID breakId) {
        return restClient.get()
                .uri("/api/v1/reconciliation/breaks/{id}", breakId)
                .retrieve()
                .body(BreakDto.class);
    }

    @Override
    public BreakDto resolve(UUID breakId, String action, String actor, String idempotencyKey) {
        return restClient.post()
                .uri("/api/v1/reconciliation/breaks/{id}/resolve", breakId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(new ResolveBody(action, actor))
                .retrieve()
                .body(BreakDto.class);
    }

    private record StartRunBody(UUID tenantId, String railCode, String statementKey) {
    }

    private record ResolveBody(String action, String actor) {
    }
}
