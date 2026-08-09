package com.payhub.merchant.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.payhub.merchant.domain.Merchant;

public interface MerchantRepository {

    Merchant save(Merchant merchant);

    Optional<Merchant> findById(UUID id);
}
