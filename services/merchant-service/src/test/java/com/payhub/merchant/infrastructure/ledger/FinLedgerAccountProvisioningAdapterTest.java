package com.payhub.merchant.infrastructure.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.payhub.merchant.application.port.out.AccountProvisioningPort;
import com.payhub.merchant.application.port.out.AccountProvisioningPort.ProvisionCommand;
import com.payhub.merchant.application.port.out.AccountProvisioningPort.ProvisionedAccounts;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@Tag("contract")
class FinLedgerAccountProvisioningAdapterTest {

    private MockWebServer server;
    private AccountProvisioningPort port;
    private final UUID aggregatorId = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        FinLedgerProvisioningProperties properties = new FinLedgerProvisioningProperties(
                server.url("/").toString().replaceAll("/$", ""),
                aggregatorId,
                "USD"
        );
        port = new FinLedgerAccountProvisioningAdapter(RestClient.builder(), properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void should_create_sub_merchant_tenant_and_wallets_with_idempotency_keys() throws Exception {
        UUID tenantId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID wallet1 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID wallet2 = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

        enqueueTenant(tenantId);
        enqueueAccount(wallet1, tenantId);
        enqueueAccount(wallet2, tenantId);
        enqueueTenant(tenantId);
        enqueueAccount(wallet1, tenantId);
        enqueueAccount(wallet2, tenantId);

        ProvisionCommand command = new ProvisionCommand("Send Tunnel Co", "approve-1", "test-token");
        ProvisionedAccounts first = port.provisionSubMerchant(command);
        ProvisionedAccounts second = port.provisionSubMerchant(command);

        assertThat(first.finLedgerTenantId()).isEqualTo(tenantId);
        assertThat(first.merchantWalletId()).isEqualTo(wallet1);
        assertThat(first.settlementWalletId()).isEqualTo(wallet2);
        assertThat(second.finLedgerTenantId()).isEqualTo(first.finLedgerTenantId());
        assertThat(second.merchantWalletId()).isEqualTo(first.merchantWalletId());
        assertThat(second.settlementWalletId()).isEqualTo(first.settlementWalletId());

        RecordedRequest tenantReq1 = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest walletReq1 = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest walletReq2 = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest tenantReq2 = server.takeRequest(1, TimeUnit.SECONDS);

        assertThat(tenantReq1).isNotNull();
        assertThat(tenantReq1.getPath()).isEqualTo("/api/v1/tenants");
        assertThat(tenantReq1.getHeader("Idempotency-Key")).isEqualTo("approve-1:tenant");
        assertThat(tenantReq1.getHeader("Authorization")).isEqualTo("Bearer test-token");
        String tenantBody = tenantReq1.getBody().readUtf8();
        assertThat(tenantBody).contains("\"type\":\"SUB_MERCHANT\"");
        assertThat(tenantBody).contains(aggregatorId.toString());

        assertThat(walletReq1.getHeader("Idempotency-Key")).isEqualTo("approve-1:wallet-merchant");
        assertThat(walletReq2.getHeader("Idempotency-Key")).isEqualTo("approve-1:wallet-settlement");
        assertThat(walletReq1.getPath()).isEqualTo("/api/v1/tenants/" + tenantId + "/accounts");

        assertThat(tenantReq2.getHeader("Idempotency-Key")).isEqualTo("approve-1:tenant");
        assertThat(server.getRequestCount()).isEqualTo(6);
    }

    private void enqueueTenant(UUID tenantId) {
        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "tenantId": "%s",
                          "name": "Send Tunnel Co",
                          "type": "SUB_MERCHANT",
                          "parentTenantId": "%s"
                        }
                        """.formatted(tenantId, aggregatorId)));
    }

    private void enqueueAccount(UUID accountId, UUID tenantId) {
        server.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "accountId": "%s",
                          "tenantId": "%s",
                          "ownerRef": "owner",
                          "currencyCode": "USD",
                          "type": "MERCHANT_WALLET",
                          "status": "ACTIVE",
                          "allowsOverdraft": false,
                          "available": "0",
                          "pending": "0",
                          "held": "0"
                        }
                        """.formatted(accountId, tenantId)));
    }
}
