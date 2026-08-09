package com.payhub.merchant.application.usecase;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.merchant.application.IllegalMerchantTransitionException;
import com.payhub.merchant.application.MerchantNotFoundException;
import com.payhub.merchant.application.ProvisioningFailedException;
import com.payhub.merchant.application.dto.ApproveMerchantCommand;
import com.payhub.merchant.application.dto.MerchantView;
import com.payhub.merchant.application.port.in.ApproveMerchantUseCase;
import com.payhub.merchant.application.port.out.AccountProvisioningPort;
import com.payhub.merchant.application.port.out.AccountProvisioningPort.ProvisionCommand;
import com.payhub.merchant.application.port.out.AccountProvisioningPort.ProvisionedAccounts;
import com.payhub.merchant.application.port.out.IdempotencyStore;
import com.payhub.merchant.application.port.out.MerchantRepository;
import com.payhub.merchant.domain.IllegalMerchantStateException;
import com.payhub.merchant.domain.Merchant;
import com.payhub.merchant.domain.MerchantStatus;
import com.payhub.merchant.domain.WalletRefs;

@Service
public class ApproveMerchantService implements ApproveMerchantUseCase {

    static final String OPERATION = "approve-merchant";

    private final MerchantRepository merchantRepository;
    private final AccountProvisioningPort accountProvisioningPort;
    private final IdempotencyStore idempotencyStore;

    public ApproveMerchantService(
            MerchantRepository merchantRepository,
            AccountProvisioningPort accountProvisioningPort,
            IdempotencyStore idempotencyStore
    ) {
        this.merchantRepository = merchantRepository;
        this.accountProvisioningPort = accountProvisioningPort;
        this.idempotencyStore = idempotencyStore;
    }

    @Override
    @Transactional
    public MerchantView execute(ApproveMerchantCommand command) {
        Objects.requireNonNull(command.idempotencyKey(), "idempotencyKey");
        Objects.requireNonNull(command.bearerToken(), "bearerToken");

        Optional<UUID> prior = idempotencyStore.findMerchantId(OPERATION, command.idempotencyKey());
        if (prior.isPresent()) {
            if (!prior.get().equals(command.merchantId())) {
                throw new com.payhub.merchant.application.IdempotencyConflictException(
                        "Idempotency-Key already used for a different merchant");
            }
            Merchant existing = merchantRepository.findById(command.merchantId())
                    .orElseThrow(() -> new MerchantNotFoundException(command.merchantId()));
            return MerchantView.from(existing);
        }

        Merchant merchant = merchantRepository.findById(command.merchantId())
                .orElseThrow(() -> new MerchantNotFoundException(command.merchantId()));

        if (merchant.status() == MerchantStatus.ACTIVE) {
            idempotencyStore.save(OPERATION, command.idempotencyKey(), command.merchantId().toString(), merchant.id());
            return MerchantView.from(merchant);
        }
        if (merchant.status() != MerchantStatus.PENDING_REVIEW) {
            throw new IllegalMerchantTransitionException(
                    "Cannot activate merchant from status " + merchant.status());
        }

        ProvisionedAccounts provisioned;
        try {
            provisioned = accountProvisioningPort.provisionSubMerchant(
                    new ProvisionCommand(
                            merchant.legalName(),
                            command.idempotencyKey(),
                            command.bearerToken()
                    )
            );
        } catch (RuntimeException ex) {
            throw new ProvisioningFailedException(
                    "FinLedger SUB_MERCHANT provisioning failed; merchant remains PENDING_REVIEW",
                    ex
            );
        }

        try {
            merchant.activate(
                    provisioned.finLedgerTenantId(),
                    new WalletRefs(provisioned.merchantWalletId(), provisioned.settlementWalletId())
            );
        } catch (IllegalMerchantStateException ex) {
            throw new IllegalMerchantTransitionException(ex.getMessage());
        }
        Merchant saved = merchantRepository.save(merchant);
        idempotencyStore.save(OPERATION, command.idempotencyKey(), command.merchantId().toString(), saved.id());
        return MerchantView.from(saved);
    }
}
