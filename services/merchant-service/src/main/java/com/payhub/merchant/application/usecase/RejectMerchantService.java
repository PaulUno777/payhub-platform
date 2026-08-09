package com.payhub.merchant.application.usecase;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.merchant.application.IdempotencyConflictException;
import com.payhub.merchant.application.IllegalMerchantTransitionException;
import com.payhub.merchant.application.MerchantNotFoundException;
import com.payhub.merchant.application.dto.MerchantView;
import com.payhub.merchant.application.dto.RejectMerchantCommand;
import com.payhub.merchant.application.port.in.RejectMerchantUseCase;
import com.payhub.merchant.application.port.out.IdempotencyStore;
import com.payhub.merchant.application.port.out.MerchantRepository;
import com.payhub.merchant.domain.IllegalMerchantStateException;
import com.payhub.merchant.domain.Merchant;
import com.payhub.merchant.domain.MerchantStatus;

@Service
public class RejectMerchantService implements RejectMerchantUseCase {

    static final String OPERATION = "reject-merchant";

    private final MerchantRepository merchantRepository;
    private final IdempotencyStore idempotencyStore;

    public RejectMerchantService(
            MerchantRepository merchantRepository,
            IdempotencyStore idempotencyStore
    ) {
        this.merchantRepository = merchantRepository;
        this.idempotencyStore = idempotencyStore;
    }

    @Override
    @Transactional
    public MerchantView execute(RejectMerchantCommand command) {
        Objects.requireNonNull(command.idempotencyKey(), "idempotencyKey");
        Objects.requireNonNull(command.reason(), "reason");

        Optional<UUID> prior = idempotencyStore.findMerchantId(OPERATION, command.idempotencyKey());
        if (prior.isPresent()) {
            if (!prior.get().equals(command.merchantId())) {
                throw new IdempotencyConflictException(
                        "Idempotency-Key already used for a different merchant");
            }
            return MerchantView.from(merchantRepository.findById(command.merchantId())
                    .orElseThrow(() -> new MerchantNotFoundException(command.merchantId())));
        }

        Merchant merchant = merchantRepository.findById(command.merchantId())
                .orElseThrow(() -> new MerchantNotFoundException(command.merchantId()));

        if (merchant.status() == MerchantStatus.REJECTED) {
            idempotencyStore.save(OPERATION, command.idempotencyKey(), command.reason(), merchant.id());
            return MerchantView.from(merchant);
        }

        try {
            merchant.reject(command.reason());
        } catch (IllegalMerchantStateException ex) {
            throw new IllegalMerchantTransitionException(ex.getMessage());
        }
        Merchant saved = merchantRepository.save(merchant);
        idempotencyStore.save(OPERATION, command.idempotencyKey(), command.reason(), saved.id());
        return MerchantView.from(saved);
    }
}
