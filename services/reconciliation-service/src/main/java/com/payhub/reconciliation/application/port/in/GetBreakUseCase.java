package com.payhub.reconciliation.application.port.in;

import java.util.UUID;

import com.payhub.reconciliation.application.dto.BreakView;

public interface GetBreakUseCase {

    BreakView execute(UUID breakId);
}
