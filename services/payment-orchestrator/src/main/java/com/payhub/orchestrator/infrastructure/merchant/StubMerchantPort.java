package com.payhub.orchestrator.infrastructure.merchant;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.payhub.orchestrator.application.port.out.MerchantPort;

@Component
public class StubMerchantPort implements MerchantPort {

    public static final String DEFAULT_RULE_SET = "ecopay-default";

    @Override
    public MerchantSnapshot findById(UUID merchantId) {
        return new MerchantSnapshot(merchantId, DEFAULT_RULE_SET, null);
    }
}
