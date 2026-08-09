package com.payhub.merchant.application.port.in;

import com.payhub.merchant.application.dto.ApproveMerchantCommand;
import com.payhub.merchant.application.dto.MerchantView;

public interface ApproveMerchantUseCase {

    MerchantView execute(ApproveMerchantCommand command);
}
