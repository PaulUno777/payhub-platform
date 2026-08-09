package com.payhub.reporting.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payhub.reporting.application.dto.JournalEntryEnvelope;
import com.payhub.reporting.application.dto.JournalEntryEnvelope.PostingSummary;
import com.payhub.reporting.application.dto.JournalEntryEnvelope.TransactionPostedPayload;
import com.payhub.messaging.inbox.InboxStore;
import com.payhub.reporting.application.port.out.JournalEntryProjectionStore;
import com.payhub.reporting.application.port.out.JournalEntryProjectionStore.JournalEntryProjection;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ApplyJournalEntryProjectionServiceTest {

    @Mock
    private InboxStore inboxStore;
    @Mock
    private JournalEntryProjectionStore projectionStore;

    private ApplyJournalEntryProjectionService service;

    @BeforeEach
    void setUp() {
        service = new ApplyJournalEntryProjectionService(inboxStore, projectionStore);
    }

    @Test
    void should_project_once_for_new_event() {
        JournalEntryEnvelope envelope = sampleEnvelope(UUID.randomUUID());
        when(inboxStore.exists(envelope.eventId())).thenReturn(false);

        boolean applied = service.execute(envelope);

        assertThat(applied).isTrue();
        ArgumentCaptor<JournalEntryProjection> captor = ArgumentCaptor.forClass(JournalEntryProjection.class);
        verify(projectionStore).upsert(captor.capture());
        assertThat(captor.getValue().journalEntryId()).isEqualTo(envelope.payload().journalEntryId());
        assertThat(captor.getValue().eventId()).isEqualTo(envelope.eventId());
        assertThat(captor.getValue().asOf()).isNotNull();
        verify(inboxStore).save(envelope.eventId(), ApplyJournalEntryProjectionService.CONSUMER);
    }

    @Test
    void should_skip_when_event_already_in_inbox() {
        JournalEntryEnvelope envelope = sampleEnvelope(UUID.randomUUID());
        when(inboxStore.exists(envelope.eventId())).thenReturn(true);

        boolean applied = service.execute(envelope);

        assertThat(applied).isFalse();
        verify(projectionStore, never()).upsert(any());
        verify(inboxStore, never()).save(any(), any());
    }

    private static JournalEntryEnvelope sampleEnvelope(UUID eventId) {
        UUID tenantId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        Instant occurred = Instant.parse("2026-08-09T00:00:00Z");
        return new JournalEntryEnvelope(
                eventId,
                "TransactionPosted",
                "1",
                occurred,
                "finledger",
                journalId,
                tenantId,
                null,
                null,
                new TransactionPostedPayload(
                        tenantId,
                        journalId,
                        "tx-1",
                        "TRANSFER",
                        List.of(new PostingSummary(UUID.randomUUID(), "10.00", "USD", "SETTLED")),
                        occurred
                )
        );
    }
}
