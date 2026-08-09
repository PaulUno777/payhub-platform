package com.payhub.reconciliation.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.reconciliation.application.BreakNotFoundException;
import com.payhub.reconciliation.application.dto.BreakView;
import com.payhub.reconciliation.application.port.in.GetBreakUseCase;
import com.payhub.reconciliation.application.port.out.BreakRepository;

@Service
public class GetBreakService implements GetBreakUseCase {

    private final BreakRepository breakRepository;

    public GetBreakService(BreakRepository breakRepository) {
        this.breakRepository = breakRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BreakView execute(UUID breakId) {
        return breakRepository.findById(breakId)
                .map(BreakView::from)
                .orElseThrow(() -> new BreakNotFoundException(breakId));
    }
}
