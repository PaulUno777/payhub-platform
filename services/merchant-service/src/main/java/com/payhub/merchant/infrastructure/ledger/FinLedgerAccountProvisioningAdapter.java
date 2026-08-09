package com.payhub.merchant.infrastructure.ledger;

import java.util.Objects;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.payhub.merchant.application.port.out.AccountProvisioningPort;

/**
 * Separate FinLedger ACL for merchant onboarding (tenant + wallets).
 * Field names mirror FinLedger CreateTenantRequest / CreateAccountRequest
 * (finledger/.../TenantController.java, LedgerAccountController.java).
 */
@Component
public class FinLedgerAccountProvisioningAdapter implements AccountProvisioningPort {

    private static final String TENANT_TYPE_SUB_MERCHANT = "SUB_MERCHANT";
    private static final String ACCOUNT_TYPE_MERCHANT_WALLET = "MERCHANT_WALLET";

    private final RestClient restClient;
    private final FinLedgerProvisioningProperties properties;

    public FinLedgerAccountProvisioningAdapter(
            RestClient.Builder restClientBuilder,
            FinLedgerProvisioningProperties properties
    ) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(Objects.requireNonNull(properties.baseUrl(), "payhub.finledger.base-url"))
                .build();
    }

    @Override
    public ProvisionedAccounts provisionSubMerchant(ProvisionCommand command) {
        Objects.requireNonNull(command.idempotencyKey(), "idempotencyKey");
        Objects.requireNonNull(command.bearerToken(), "bearerToken");

        CreateTenantResponse tenant = restClient.post()
                .uri("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", command.idempotencyKey() + ":tenant")
                .header("Authorization", "Bearer " + command.bearerToken())
                .body(new CreateTenantRequest(
                        command.legalName(),
                        TENANT_TYPE_SUB_MERCHANT,
                        properties.aggregatorTenantId(),
                        null
                ))
                .retrieve()
                .body(CreateTenantResponse.class);

        if (tenant == null || tenant.tenantId() == null) {
            throw new IllegalStateException("FinLedger create tenant returned empty body");
        }

        UUID tenantId = tenant.tenantId();
        String ownerBase = slug(command.legalName());

        CreateAccountResponse merchantWallet = createWallet(
                tenantId,
                ownerBase + "-merchant",
                command.idempotencyKey() + ":wallet-merchant",
                command.bearerToken()
        );
        CreateAccountResponse settlementWallet = createWallet(
                tenantId,
                ownerBase + "-settlement",
                command.idempotencyKey() + ":wallet-settlement",
                command.bearerToken()
        );

        return new ProvisionedAccounts(
                tenantId,
                merchantWallet.accountId(),
                settlementWallet.accountId()
        );
    }

    private CreateAccountResponse createWallet(
            UUID tenantId,
            String ownerRef,
            String idempotencyKey,
            String bearerToken
    ) {
        CreateAccountResponse body = restClient.post()
                .uri("/api/v1/tenants/{tenantId}/accounts", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .header("Authorization", "Bearer " + bearerToken)
                .body(new CreateAccountRequest(
                        ownerRef,
                        properties.defaultCurrency(),
                        ACCOUNT_TYPE_MERCHANT_WALLET,
                        Boolean.FALSE
                ))
                .retrieve()
                .body(CreateAccountResponse.class);
        if (body == null || body.accountId() == null) {
            throw new IllegalStateException("FinLedger create account returned empty body");
        }
        return body;
    }

    private static String slug(String legalName) {
        String slug = legalName.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("^-+|-+$", "");
        if (slug.isBlank()) {
            return "merchant";
        }
        return slug.length() > 40 ? slug.substring(0, 40) : slug;
    }

    record CreateTenantRequest(String name, String type, UUID parentTenantId, UUID id) {
    }

    record CreateTenantResponse(UUID tenantId, String name, String type, UUID parentTenantId) {
    }

    record CreateAccountRequest(
            String ownerRef,
            String currencyCode,
            String type,
            Boolean allowsOverdraft
    ) {
    }

    record CreateAccountResponse(
            UUID accountId,
            UUID tenantId,
            String ownerRef,
            String currencyCode,
            String type,
            String status,
            boolean allowsOverdraft,
            String available,
            String pending,
            String held
    ) {
    }
}
