package com.payhub.orchestrator.application;

/**
 * Outbound dependency failed in a way that is safe to retry (plan §7.2).
 * Never use for rail timeouts that must become {@code RECONCILIATION_REQUIRED} /
 * {@code AMBIGUOUS} — those stay rail-outcome mapped.
 */
public class RetryableDependencyException extends RuntimeException {

    private final String dependency;

    public RetryableDependencyException(String dependency, String message, Throwable cause) {
        super(message, cause);
        this.dependency = dependency;
    }

    public String dependency() {
        return dependency;
    }

    public String classification() {
        return "retryable";
    }
}
