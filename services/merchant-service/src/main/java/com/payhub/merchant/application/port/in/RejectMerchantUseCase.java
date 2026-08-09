package com.payhub.merchant.application.port.in;

import com.payhub.merchant.application.dto.MerchantView;
import com.payhub.merchant.application.dto.RejectMerchantCommand;

public interface RejectMerchantUseCase {

    MerchantView execute(RejectMerchantCommand command);
}
