package com.payhub.orchestrator.infrastructure.resilience;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.payhub.orchestrator.infrastructure.ledger.FinLedgerProperties;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.observation.ObservationRegistry;

@Configuration
@EnableConfigurationProperties({ResilienceProperties.class, FinLedgerProperties.class})
public class OutboundResilienceConfig {

    public static final String RAIL_CLIENT = "railRestClient";
    public static final String FIN_LEDGER_CLIENT = "finLedgerRestClient";
    public static final String RISK_CLIENT = "riskRestClient";

    @Bean(destroyMethod = "shutdown")
    ScheduledExecutorService resilienceScheduler() {
        AtomicInteger seq = new AtomicInteger();
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "payhub-resilience-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4, factory);
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    @Bean
    BulkheadRegistry bulkheadRegistry(ResilienceProperties properties) {
        BulkheadRegistry registry = BulkheadRegistry.ofDefaults();
        properties.asMap().forEach((name, cfg) -> registry.bulkhead(name, BulkheadConfig.custom()
                .maxConcurrentCalls(cfg.bulkheadMaxConcurrent())
                .maxWaitDuration(cfg.bulkheadMaxWait())
                .build()));
        return registry;
    }

    @Bean
    CircuitBreakerRegistry circuitBreakerRegistry(ResilienceProperties properties) {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        properties.asMap().forEach((name, cfg) -> registry.circuitBreaker(name, CircuitBreakerConfig.custom()
                .failureRateThreshold(cfg.failureRateThreshold())
                .slowCallRateThreshold(cfg.failureRateThreshold())
                .slowCallDurationThreshold(cfg.slowCallDuration())
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(20)
                .minimumNumberOfCalls(5)
                .build()));
        return registry;
    }

    @Bean
    TimeLimiterRegistry timeLimiterRegistry(ResilienceProperties properties) {
        TimeLimiterRegistry registry = TimeLimiterRegistry.ofDefaults();
        properties.asMap().forEach((name, cfg) -> registry.timeLimiter(name, TimeLimiterConfig.custom()
                .timeoutDuration(cfg.timeout())
                .cancelRunningFuture(true)
                .build()));
        return registry;
    }

    @Bean
    RetryRegistry retryRegistry(ResilienceProperties properties) {
        RetryRegistry registry = RetryRegistry.ofDefaults();
        properties.asMap().forEach((name, cfg) -> {
            if (!cfg.retryEnabled()) {
                return;
            }
            registry.retry(name, RetryConfig.custom()
                    .maxAttempts(cfg.retryMaxAttempts())
                    .intervalFunction(IntervalFunction.ofExponentialBackoff(100, 2.0))
                    .retryOnException(ex -> ex instanceof java.io.IOException
                            || (ex.getCause() instanceof java.io.IOException))
                    .build());
        });
        return registry;
    }

    @Bean
    DependencyResilience dependencyResilience(
            BulkheadRegistry bulkheadRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry,
            RetryRegistry retryRegistry,
            ScheduledExecutorService resilienceScheduler,
            ResilienceProperties properties
    ) {
        return new DependencyResilience(
                bulkheadRegistry,
                circuitBreakerRegistry,
                timeLimiterRegistry,
                retryRegistry,
                resilienceScheduler,
                properties
        );
    }

    @Bean(name = RAIL_CLIENT)
    RestClient railRestClient(
            ResilienceProperties properties,
            ObjectProvider<ObservationRegistry> observationRegistry,
            @Value("${payhub.clients.rail-adapter-service}") String baseUrl
    ) {
        return buildClient(properties.rail(), baseUrl, observationRegistry);
    }

    @Bean(name = FIN_LEDGER_CLIENT)
    RestClient finLedgerRestClient(
            ResilienceProperties properties,
            FinLedgerProperties finLedgerProperties,
            ObjectProvider<ObservationRegistry> observationRegistry
    ) {
        return buildClient(properties.finledger(), finLedgerProperties.baseUrl(), observationRegistry);
    }

    @Bean(name = RISK_CLIENT)
    RestClient riskRestClient(
            ResilienceProperties properties,
            ObjectProvider<ObservationRegistry> observationRegistry,
            @Value("${payhub.clients.risk-service}") String baseUrl
    ) {
        return buildClient(properties.risk(), baseUrl, observationRegistry);
    }

    static RestClient buildClient(
            ResilienceProperties.DependencyConfig cfg,
            String baseUrl
    ) {
        return buildClient(cfg, baseUrl, new ObjectProvider<>() {
            @Override
            public ObservationRegistry getObject() {
                return null;
            }

            @Override
            public ObservationRegistry getIfAvailable() {
                return null;
            }

            @Override
            public ObservationRegistry getIfUnique() {
                return null;
            }
        });
    }

    static RestClient buildClient(
            ResilienceProperties.DependencyConfig cfg,
            String baseUrl,
            ObjectProvider<ObservationRegistry> observationRegistry
    ) {
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(cfg.maxConnTotal())
                .setMaxConnPerRoute(cfg.maxConnPerRoute())
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.of(cfg.connectTimeout().toMillis(), TimeUnit.MILLISECONDS))
                        .setSocketTimeout(Timeout.of(cfg.timeout().toMillis(), TimeUnit.MILLISECONDS))
                        .build())
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(cfg.connectTimeout().toMillis(), TimeUnit.MILLISECONDS))
                .setResponseTimeout(Timeout.of(cfg.timeout().toMillis(), TimeUnit.MILLISECONDS))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectionRequestTimeout(cfg.connectTimeout());
        requestFactory.setReadTimeout(cfg.timeout());

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(baseUrl);
        observationRegistry.ifAvailable(builder::observationRegistry);
        return builder.build();
    }

    /**
     * Decorates outbound calls with Bulkhead + CircuitBreaker + TimeLimiter (+ optional Retry).
     */
    public static final class DependencyResilience {

        private final BulkheadRegistry bulkheadRegistry;
        private final CircuitBreakerRegistry circuitBreakerRegistry;
        private final TimeLimiterRegistry timeLimiterRegistry;
        private final RetryRegistry retryRegistry;
        private final ScheduledExecutorService scheduler;
        private final ResilienceProperties properties;

        public DependencyResilience(
                BulkheadRegistry bulkheadRegistry,
                CircuitBreakerRegistry circuitBreakerRegistry,
                TimeLimiterRegistry timeLimiterRegistry,
                RetryRegistry retryRegistry,
                ScheduledExecutorService scheduler,
                ResilienceProperties properties
        ) {
            this.bulkheadRegistry = bulkheadRegistry;
            this.circuitBreakerRegistry = circuitBreakerRegistry;
            this.timeLimiterRegistry = timeLimiterRegistry;
            this.retryRegistry = retryRegistry;
            this.scheduler = scheduler;
            this.properties = properties;
        }

        public <T> T execute(String dependency, Supplier<T> supplier) {
            Bulkhead bulkhead = bulkheadRegistry.bulkhead(dependency);
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(dependency);
            TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter(dependency);

            Supplier<T> call = supplier;
            if (properties.forName(dependency).retryEnabled() && retryRegistry.find(dependency).isPresent()) {
                call = Retry.decorateSupplier(retryRegistry.retry(dependency), call);
            }
            Supplier<T> withBreaker = CircuitBreaker.decorateSupplier(circuitBreaker, call);

            // Bulkhead on the caller thread so BulkheadFullException is immediate / mappable.
            return Bulkhead.decorateSupplier(bulkhead, () -> {
                try {
                    return TimeLimiter.decorateFutureSupplier(
                            timeLimiter,
                            () -> java.util.concurrent.CompletableFuture.supplyAsync(withBreaker, scheduler)
                    ).call();
                } catch (Exception ex) {
                    if (ex instanceof RuntimeException runtime) {
                        throw runtime;
                    }
                    throw new RuntimeException(ex);
                }
            }).get();
        }
    }
}
