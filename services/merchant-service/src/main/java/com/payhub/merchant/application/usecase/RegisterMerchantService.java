package com.payhub.merchant.application.usecase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.merchant.application.IdempotencyConflictException;
import com.payhub.merchant.application.dto.MerchantView;
import com.payhub.merchant.application.dto.RegisterMerchantCommand;
import com.payhub.merchant.application.port.in.RegisterMerchantUseCase;
import com.payhub.merchant.application.port.out.IdempotencyStore;
import com.payhub.merchant.application.port.out.MerchantRepository;
import com.payhub.merchant.domain.Merchant;

@Service
public class RegisterMerchantService implements RegisterMerchantUseCase {

    static final String OPERATION = "register-merchant";

    private final MerchantRepository merchantRepository;
    private final IdempotencyStore idempotencyStore;

    public RegisterMerchantService(
            MerchantRepository merchantRepository,
            IdempotencyStore idempotencyStore
    ) {
        this.merchantRepository = merchantRepository;
        this.idempotencyStore = idempotencyStore;
    }

    @Override
    @Transactional
    public MerchantView execute(RegisterMerchantCommand command) {
        Objects.requireNonNull(command.idempotencyKey(), "idempotencyKey");
        var tier = com.payhub.merchant.domain.MerchantTier.valueOf(command.tier());
        String hash = requestHash(command.legalName(), tier.name(), command.assignedRuleSetKey());

        Optional<UUID> existing = idempotencyStore.findMerchantId(OPERATION, command.idempotencyKey());
        if (existing.isPresent()) {
            Merchant merchant = merchantRepository.findById(existing.get())
                    .orElseThrow(() -> new IllegalStateException("Idempotency points to missing merchant"));
            return MerchantView.from(merchant);
        }

        Merchant merchant = Merchant.register(
                command.legalName(),
                tier,
                command.assignedRuleSetKey()
        );
        Merchant saved = merchantRepository.save(merchant);
        try {
            idempotencyStore.save(OPERATION, command.idempotencyKey(), hash, saved.id());
        } catch (RuntimeException ex) {
            Optional<UUID> raced = idempotencyStore.findMerchantId(OPERATION, command.idempotencyKey());
            if (raced.isPresent()) {
                return MerchantView.from(merchantRepository.findById(raced.get()).orElseThrow());
            }
            throw new IdempotencyConflictException("Idempotency-Key already used with a different payload");
        }
        return MerchantView.from(saved);
    }

    static String requestHash(String legalName, String tier, String ruleSetKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((legalName + "|" + tier + "|" + ruleSetKey)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
