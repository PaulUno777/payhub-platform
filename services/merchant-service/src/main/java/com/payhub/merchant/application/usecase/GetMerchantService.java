package com.payhub.merchant.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.merchant.application.MerchantNotFoundException;
import com.payhub.merchant.application.dto.MerchantView;
import com.payhub.merchant.application.port.in.GetMerchantUseCase;
import com.payhub.merchant.application.port.out.MerchantRepository;

@Service
public class GetMerchantService implements GetMerchantUseCase {

    private final MerchantRepository merchantRepository;

    public GetMerchantService(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantView execute(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .map(MerchantView::from)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId));
    }
}
