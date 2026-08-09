package com.payhub.reporting.application.port.in;

public interface ReplayDlqEventsUseCase {

    /**
     * @return number of records republished to the original topic
     */
    int execute(String dlqTopic, int maxRecords);
}
