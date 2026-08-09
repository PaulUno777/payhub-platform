package com.payhub.reporting.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.payhub.reporting.application.port.out.DlqReplayPort;

@Tag("unit")
class ReplayDlqEventsServiceTest {

    @Test
    void should_republish_via_port_when_dlq_topic_valid() {
        DlqReplayPort port = mock(DlqReplayPort.class);
        when(port.republish(eq("ledger.journal-entry.v1.dlq"), eq(10))).thenReturn(3);
        ReplayDlqEventsService service = new ReplayDlqEventsService(port);

        assertThat(service.execute("ledger.journal-entry.v1.dlq", 10)).isEqualTo(3);
        verify(port).republish("ledger.journal-entry.v1.dlq", 10);
    }

    @Test
    void should_reject_non_dlq_topic() {
        ReplayDlqEventsService service = new ReplayDlqEventsService(mock(DlqReplayPort.class));
        assertThatThrownBy(() -> service.execute("ledger.journal-entry.v1", 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_cap_max_records() {
        DlqReplayPort port = mock(DlqReplayPort.class);
        when(port.republish(eq("payment.lifecycle.v1.dlq"), eq(500))).thenReturn(0);
        ReplayDlqEventsService service = new ReplayDlqEventsService(port);

        service.execute("payment.lifecycle.v1.dlq", 10_000);
        verify(port).republish("payment.lifecycle.v1.dlq", 500);
    }
}
