package com.payhub.merchant.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.payhub.merchant.TestcontainersConfiguration;
import com.payhub.merchant.application.port.out.MerchantRepository;
import com.payhub.merchant.domain.Merchant;
import com.payhub.merchant.domain.MerchantStatus;
import com.payhub.merchant.domain.MerchantTier;
import com.payhub.merchant.domain.WalletRefs;

@Tag("integration")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MerchantRepositoryIT {

    @Autowired
    private MerchantRepository merchantRepository;

    @Test
    void should_round_trip_pending_then_active_merchant() {
        Merchant pending = Merchant.register("Acme IT", MerchantTier.PREMIUM, "ecopay-default");
        Merchant savedPending = merchantRepository.save(pending);

        Merchant loaded = merchantRepository.findById(savedPending.id()).orElseThrow();
        assertThat(loaded.status()).isEqualTo(MerchantStatus.PENDING_REVIEW);
        assertThat(loaded.legalName()).isEqualTo("Acme IT");

        UUID tenantId = UUID.randomUUID();
        WalletRefs wallets = new WalletRefs(UUID.randomUUID(), UUID.randomUUID());
        loaded.activate(tenantId, wallets);
        merchantRepository.save(loaded);

        Merchant active = merchantRepository.findById(savedPending.id()).orElseThrow();
        assertThat(active.status()).isEqualTo(MerchantStatus.ACTIVE);
        assertThat(active.finLedgerTenantId()).isEqualTo(tenantId);
        assertThat(active.walletRefs()).isEqualTo(wallets);
        assertThat(active.assignedRuleSetKey()).isEqualTo("ecopay-default");
    }
}
