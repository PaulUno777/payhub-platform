package com.payhub.reporting.application.usecase;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.payhub.messaging.kafka.KafkaPoisonHandlers;
import com.payhub.reporting.application.port.in.ReplayDlqEventsUseCase;
import com.payhub.reporting.application.port.out.DlqReplayPort;

@Service
@ConditionalOnProperty(prefix = "payhub.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReplayDlqEventsService implements ReplayDlqEventsUseCase {

    private static final int DEFAULT_MAX = 50;
    private static final int HARD_MAX = 500;

    private final DlqReplayPort dlqReplayPort;

    public ReplayDlqEventsService(DlqReplayPort dlqReplayPort) {
        this.dlqReplayPort = dlqReplayPort;
    }

    @Override
    public int execute(String dlqTopic, int maxRecords) {
        KafkaPoisonHandlers.originalTopicFromDlq(dlqTopic);
        int limit = maxRecords <= 0 ? DEFAULT_MAX : Math.min(maxRecords, HARD_MAX);
        return dlqReplayPort.republish(dlqTopic, limit);
    }
}
