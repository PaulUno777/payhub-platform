package com.payhub.merchant.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payhub.merchant.application.IllegalMerchantTransitionException;
import com.payhub.merchant.application.ProvisioningFailedException;
import com.payhub.merchant.application.dto.ApproveMerchantCommand;
import com.payhub.merchant.application.dto.MerchantView;
import com.payhub.merchant.application.port.out.AccountProvisioningPort;
import com.payhub.merchant.application.port.out.AccountProvisioningPort.ProvisionCommand;
import com.payhub.merchant.application.port.out.AccountProvisioningPort.ProvisionedAccounts;
import com.payhub.merchant.application.port.out.IdempotencyStore;
import com.payhub.merchant.application.port.out.MerchantRepository;
import com.payhub.merchant.domain.Merchant;
import com.payhub.merchant.domain.MerchantStatus;
import com.payhub.merchant.domain.MerchantTier;
import com.payhub.merchant.domain.WalletRefs;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ApproveMerchantServiceTest {

    @Mock
    private MerchantRepository merchantRepository;
    @Mock
    private AccountProvisioningPort accountProvisioningPort;
    @Mock
    private IdempotencyStore idempotencyStore;

    private ApproveMerchantService service;
    private final Map<UUID, Merchant> store = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new ApproveMerchantService(merchantRepository, accountProvisioningPort, idempotencyStore);
        lenient().when(merchantRepository.save(any())).thenAnswer(inv -> {
            Merchant m = inv.getArgument(0);
            store.put(m.id(), m);
            return m;
        });
        when(merchantRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0))));
        lenient().when(idempotencyStore.findMerchantId(any(), any())).thenReturn(Optional.empty());
        lenient().doNothing().when(idempotencyStore).save(any(), any(), any(), any());
    }

    @Test
    void should_provision_and_activate_pending_merchant() {
        Merchant merchant = Merchant.register("Acme", MerchantTier.STANDARD, "ecopay-default");
        store.put(merchant.id(), merchant);
        UUID tenantId = UUID.randomUUID();
        UUID wallet1 = UUID.randomUUID();
        UUID wallet2 = UUID.randomUUID();
        when(accountProvisioningPort.provisionSubMerchant(any(ProvisionCommand.class)))
                .thenReturn(new ProvisionedAccounts(tenantId, wallet1, wallet2));

        MerchantView view = service.execute(new ApproveMerchantCommand(merchant.id(), "key-1", "token"));

        assertThat(view.status()).isEqualTo(MerchantStatus.ACTIVE.name());
        assertThat(view.finLedgerTenantId()).isEqualTo(tenantId);
        assertThat(view.merchantWalletId()).isEqualTo(wallet1);
        assertThat(view.settlementWalletId()).isEqualTo(wallet2);
        verify(idempotencyStore).save(ApproveMerchantService.OPERATION, "key-1", merchant.id().toString(), merchant.id());
    }

    @Test
    void should_leave_pending_when_provisioning_fails() {
        Merchant merchant = Merchant.register("Acme", MerchantTier.STANDARD, "ecopay-default");
        store.put(merchant.id(), merchant);
        when(accountProvisioningPort.provisionSubMerchant(any()))
                .thenThrow(new RuntimeException("finledger down"));

        assertThatThrownBy(() -> service.execute(new ApproveMerchantCommand(merchant.id(), "key-1", "token")))
                .isInstanceOf(ProvisioningFailedException.class);
        assertThat(store.get(merchant.id()).status()).isEqualTo(MerchantStatus.PENDING_REVIEW);
        verify(merchantRepository, never()).save(any());
    }

    @Test
    void should_reject_approve_when_already_rejected() {
        Merchant merchant = Merchant.register("Acme", MerchantTier.STANDARD, "ecopay-default");
        merchant.reject("no");
        store.put(merchant.id(), merchant);

        assertThatThrownBy(() -> service.execute(new ApproveMerchantCommand(merchant.id(), "key-1", "token")))
                .isInstanceOf(IllegalMerchantTransitionException.class);
        verify(accountProvisioningPort, never()).provisionSubMerchant(any());
    }

    @Test
    void should_return_active_merchant_without_reprovisioning() {
        Merchant merchant = Merchant.register("Acme", MerchantTier.STANDARD, "ecopay-default");
        UUID tenantId = UUID.randomUUID();
        merchant.activate(tenantId, new WalletRefs(UUID.randomUUID(), UUID.randomUUID()));
        store.put(merchant.id(), merchant);

        MerchantView view = service.execute(new ApproveMerchantCommand(merchant.id(), "key-1", "token"));

        assertThat(view.status()).isEqualTo(MerchantStatus.ACTIVE.name());
        assertThat(view.finLedgerTenantId()).isEqualTo(tenantId);
        verify(accountProvisioningPort, never()).provisionSubMerchant(any());
    }
}
