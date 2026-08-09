package com.payhub.reporting.adapter.in.rest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.payhub.reporting.application.port.in.ReplayDlqEventsUseCase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/reporting/events/dlq")
@ConditionalOnProperty(prefix = "payhub.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DlqReplayController {

    private final ReplayDlqEventsUseCase replayDlqEventsUseCase;

    public DlqReplayController(ReplayDlqEventsUseCase replayDlqEventsUseCase) {
        this.replayDlqEventsUseCase = replayDlqEventsUseCase;
    }

    @PostMapping("/replay")
    public ReplayResponse replay(@Valid @RequestBody ReplayRequest request) {
        try {
            int republished = replayDlqEventsUseCase.execute(request.dlqTopic(), request.maxRecords());
            return new ReplayResponse(request.dlqTopic(), republished);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    public record ReplayRequest(
            @NotBlank String dlqTopic,
            @Min(1) @Max(500) int maxRecords
    ) {
        public ReplayRequest {
            if (maxRecords <= 0) {
                maxRecords = 50;
            }
        }
    }

    public record ReplayResponse(String dlqTopic, int republished) {
    }
}
