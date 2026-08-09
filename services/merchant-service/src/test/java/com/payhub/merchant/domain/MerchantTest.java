package com.payhub.merchant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class MerchantTest {

    @Test
    void should_start_in_pending_review_when_registered() {
        Merchant merchant = Merchant.register("Acme", MerchantTier.STANDARD, "ecopay-default");

        assertThat(merchant.status()).isEqualTo(MerchantStatus.PENDING_REVIEW);
        assertThat(merchant.finLedgerTenantId()).isNull();
        assertThat(merchant.walletRefs()).isNull();
    }

    @Test
    void should_activate_from_pending_review_with_finledger_ids() {
        Merchant merchant = Merchant.register("Acme", MerchantTier.STANDARD, "ecopay-default");
        UUID tenantId = UUID.randomUUID();
        WalletRefs wallets = new WalletRefs(UUID.randomUUID(), UUID.randomUUID());

        merchant.activate(tenantId, wallets);

        assertThat(merchant.status()).isEqualTo(MerchantStatus.ACTIVE);
        assertThat(merchant.finLedgerTenantId()).isEqualTo(tenantId);
        assertThat(merchant.walletRefs()).isEqualTo(wallets);
    }

    @Test
    void should_reject_from_pending_review() {
        Merchant merchant = Merchant.register("Acme", MerchantTier.STANDARD, "ecopay-default");

        merchant.reject("kyc-failed");

        assertThat(merchant.status()).isEqualTo(MerchantStatus.REJECTED);
        assertThat(merchant.rejectionReason()).isEqualTo("kyc-failed");
        assertThat(merchant.finLedgerTenantId()).isNull();
    }

    @Test
    void should_reject_activate_when_not_pending_review() {
        Merchant merchant = Merchant.register("Acme", MerchantTier.STANDARD, "ecopay-default");
        merchant.reject("nope");

        assertThatThrownBy(() -> merchant.activate(UUID.randomUUID(),
                new WalletRefs(UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(IllegalMerchantStateException.class);
    }

    @Test
    void should_reject_reject_when_already_active() {
        Merchant merchant = Merchant.register("Acme", MerchantTier.STANDARD, "ecopay-default");
        merchant.activate(UUID.randomUUID(), new WalletRefs(UUID.randomUUID(), UUID.randomUUID()));

        assertThatThrownBy(() -> merchant.reject("late"))
                .isInstanceOf(IllegalMerchantStateException.class);
    }

    @Test
    void should_be_idempotent_activate_with_same_ids() {
        Merchant merchant = Merchant.register("Acme", MerchantTier.STANDARD, "ecopay-default");
        UUID tenantId = UUID.randomUUID();
        WalletRefs wallets = new WalletRefs(UUID.randomUUID(), UUID.randomUUID());
        merchant.activate(tenantId, wallets);

        merchant.activate(tenantId, wallets);

        assertThat(merchant.status()).isEqualTo(MerchantStatus.ACTIVE);
    }
}
