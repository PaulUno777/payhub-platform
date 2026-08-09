package com.payhub.opsbff.application.port.in;

import java.util.UUID;

import com.payhub.opsbff.application.port.out.MerchantServicePort.MerchantDto;

public interface MerchantOpsUseCase {

    MerchantDto get(UUID merchantId);

    MerchantDto approve(UUID merchantId, String idempotencyKey, String authorizationHeader);

    MerchantDto reject(UUID merchantId, String reason, String idempotencyKey);
}
