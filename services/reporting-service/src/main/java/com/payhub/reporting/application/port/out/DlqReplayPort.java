package com.payhub.reporting.application.port.out;

/**
 * Republish records from a Kafka {@code *.dlq} topic back to the original topic (DS-016).
 */
public interface DlqReplayPort {

    /**
     * @param dlqTopic topic ending in {@code .dlq}
     * @param maxRecords upper bound of records to republish in this invocation
     * @return number of records successfully republished
     */
    int republish(String dlqTopic, int maxRecords);
}
