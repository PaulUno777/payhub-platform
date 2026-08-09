package com.payhub.reconciliation.application.port.in;

import java.util.List;
import java.util.UUID;

import com.payhub.reconciliation.application.dto.BreakView;

public interface ListBreaksUseCase {

    List<BreakView> byRun(UUID runId);

    List<BreakView> openBreaks();
}
