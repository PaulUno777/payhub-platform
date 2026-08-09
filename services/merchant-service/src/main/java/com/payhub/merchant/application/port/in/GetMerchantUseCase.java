package com.payhub.merchant.application.port.in;

import java.util.UUID;

import com.payhub.merchant.application.dto.MerchantView;

public interface GetMerchantUseCase {

    MerchantView execute(UUID merchantId);
}
