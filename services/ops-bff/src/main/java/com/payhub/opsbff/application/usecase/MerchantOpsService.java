package com.payhub.opsbff.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.payhub.opsbff.application.port.in.MerchantOpsUseCase;
import com.payhub.opsbff.application.port.out.MerchantServicePort;
import com.payhub.opsbff.application.port.out.MerchantServicePort.MerchantDto;

@Service
public class MerchantOpsService implements MerchantOpsUseCase {

    private final MerchantServicePort merchantServicePort;

    public MerchantOpsService(MerchantServicePort merchantServicePort) {
        this.merchantServicePort = merchantServicePort;
    }

    @Override
    public MerchantDto get(UUID merchantId) {
        return merchantServicePort.get(merchantId);
    }

    @Override
    public MerchantDto approve(UUID merchantId, String idempotencyKey, String authorizationHeader) {
        return merchantServicePort.approve(merchantId, idempotencyKey, authorizationHeader);
    }

    @Override
    public MerchantDto reject(UUID merchantId, String reason, String idempotencyKey) {
        return merchantServicePort.reject(merchantId, reason, idempotencyKey);
    }
}
