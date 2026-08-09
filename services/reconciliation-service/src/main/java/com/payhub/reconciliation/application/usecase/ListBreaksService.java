package com.payhub.reconciliation.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.reconciliation.application.dto.BreakView;
import com.payhub.reconciliation.application.port.in.ListBreaksUseCase;
import com.payhub.reconciliation.application.port.out.BreakRepository;
import com.payhub.reconciliation.domain.BreakStatus;

@Service
public class ListBreaksService implements ListBreaksUseCase {

    private final BreakRepository breakRepository;

    public ListBreaksService(BreakRepository breakRepository) {
        this.breakRepository = breakRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BreakView> byRun(UUID runId) {
        return breakRepository.findByRunId(runId).stream().map(BreakView::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BreakView> openBreaks() {
        return breakRepository.findByStatus(BreakStatus.OPEN).stream().map(BreakView::from).toList();
    }
}
