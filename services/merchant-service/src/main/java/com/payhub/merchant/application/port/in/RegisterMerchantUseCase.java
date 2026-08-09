package com.payhub.merchant.application.port.in;

import com.payhub.merchant.application.dto.MerchantView;
import com.payhub.merchant.application.dto.RegisterMerchantCommand;

public interface RegisterMerchantUseCase {

    MerchantView execute(RegisterMerchantCommand command);
}
