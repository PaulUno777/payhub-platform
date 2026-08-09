package com.payhub.merchant.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.payhub.merchant.application.port.out.MerchantRepository;
import com.payhub.merchant.domain.Merchant;

@Component
public class JpaMerchantRepository implements MerchantRepository {

    private final MerchantJpaRepository jpaRepository;

    public JpaMerchantRepository(MerchantJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Merchant save(Merchant merchant) {
        return MerchantMapper.toDomain(jpaRepository.save(MerchantMapper.toEntity(merchant)));
    }

    @Override
    public Optional<Merchant> findById(UUID id) {
        return jpaRepository.findById(id).map(MerchantMapper::toDomain);
    }
}
