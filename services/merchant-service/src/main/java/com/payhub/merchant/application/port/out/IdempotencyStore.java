package com.payhub.merchant.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyStore {

    Optional<UUID> findMerchantId(String operation, String idempotencyKey);

    void save(String operation, String idempotencyKey, String requestHash, UUID merchantId);
}
