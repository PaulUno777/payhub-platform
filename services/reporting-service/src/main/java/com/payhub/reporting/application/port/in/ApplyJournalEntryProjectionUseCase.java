package com.payhub.reporting.application.port.in;

import com.payhub.reporting.application.dto.JournalEntryEnvelope;

public interface ApplyJournalEntryProjectionUseCase {

    /**
     * @return true if a new projection row was written; false if inbox skip (replay)
     */
    boolean execute(JournalEntryEnvelope envelope);
}
