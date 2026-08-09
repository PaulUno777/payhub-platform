package com.payhub.merchant.domain;

import java.util.Objects;
import java.util.UUID;

public record WalletRefs(UUID merchantWalletId, UUID settlementWalletId) {

    public WalletRefs {
        Objects.requireNonNull(merchantWalletId, "merchantWalletId");
        Objects.requireNonNull(settlementWalletId, "settlementWalletId");
    }
}
