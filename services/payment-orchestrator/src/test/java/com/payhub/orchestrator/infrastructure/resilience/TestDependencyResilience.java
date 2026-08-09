package com.payhub.orchestrator.infrastructure.resilience;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;

/**
 * Builds a {@link OutboundResilienceConfig.DependencyResilience} for tests without Spring.
 * Callers must {@link #close()} to shut down the scheduler.
 */
public final class TestDependencyResilience implements AutoCloseable {

    private final OutboundResilienceConfig.DependencyResilience resilience;
    private final ScheduledExecutorService scheduler;

    private TestDependencyResilience(
            OutboundResilienceConfig.DependencyResilience resilience,
            ScheduledExecutorService scheduler
    ) {
        this.resilience = resilience;
        this.scheduler = scheduler;
    }

    public OutboundResilienceConfig.DependencyResilience resilience() {
        return resilience;
    }

    public static TestDependencyResilience permissive() {
        return create(
                new ResilienceProperties.DependencyConfig(
                        32, 16, 32, Duration.ZERO, Duration.ofSeconds(30), Duration.ofSeconds(5),
                        Duration.ofSeconds(10), 100f, 1, false
                ),
                new ResilienceProperties.DependencyConfig(
                        32, 16, 32, Duration.ZERO, Duration.ofSeconds(30), Duration.ofSeconds(5),
                        Duration.ofSeconds(10), 100f, 1, false
                ),
                new ResilienceProperties.DependencyConfig(
                        32, 16, 32, Duration.ZERO, Duration.ofSeconds(30), Duration.ofSeconds(5),
                        Duration.ofSeconds(10), 100f, 1, false
                )
        );
    }

    public static TestDependencyResilience create(
            ResilienceProperties.DependencyConfig rail,
            ResilienceProperties.DependencyConfig finledger,
            ResilienceProperties.DependencyConfig risk
    ) {
        ResilienceProperties properties = new ResilienceProperties(rail, finledger, risk);
        BulkheadRegistry bulkheads = BulkheadRegistry.ofDefaults();
        CircuitBreakerRegistry breakers = CircuitBreakerRegistry.ofDefaults();
        TimeLimiterRegistry timers = TimeLimiterRegistry.ofDefaults();
        RetryRegistry retries = RetryRegistry.ofDefaults();

        properties.asMap().forEach((name, cfg) -> {
            bulkheads.bulkhead(name, BulkheadConfig.custom()
                    .maxConcurrentCalls(cfg.bulkheadMaxConcurrent())
                    .maxWaitDuration(cfg.bulkheadMaxWait())
                    .build());
            breakers.circuitBreaker(name, CircuitBreakerConfig.custom()
                    .failureRateThreshold(cfg.failureRateThreshold())
                    .slowCallRateThreshold(100f)
                    .slowCallDurationThreshold(cfg.slowCallDuration())
                    .minimumNumberOfCalls(100)
                    .slidingWindowSize(100)
                    .build());
            timers.timeLimiter(name, TimeLimiterConfig.custom()
                    .timeoutDuration(cfg.timeout())
                    .cancelRunningFuture(true)
                    .build());
        });

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
        OutboundResilienceConfig.DependencyResilience resilience =
                new OutboundResilienceConfig.DependencyResilience(
                        bulkheads, breakers, timers, retries, scheduler, properties
                );
        return new TestDependencyResilience(resilience, scheduler);
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        try {
            scheduler.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
