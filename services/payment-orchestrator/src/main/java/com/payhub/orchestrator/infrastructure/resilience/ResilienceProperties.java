package com.payhub.orchestrator.infrastructure.resilience;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payhub.resilience")
public record ResilienceProperties(
        DependencyConfig rail,
        DependencyConfig finledger,
        DependencyConfig risk
) {

	public ResilienceProperties {
		if (rail == null) {
			rail = DependencyConfig.defaults(4, 2, 2, Duration.ofSeconds(3), Duration.ofSeconds(2), false);
		}
		if (finledger == null) {
			finledger = DependencyConfig.defaults(16, 8, 8, Duration.ofSeconds(5), Duration.ofSeconds(2), true);
		}
		if (risk == null) {
			risk = DependencyConfig.defaults(8, 4, 4, Duration.ofSeconds(1), Duration.ofMillis(800), true);
		}
	}

    public DependencyConfig forName(String name) {
        return switch (name) {
            case "rail" -> rail;
            case "finledger" -> finledger;
            case "risk" -> risk;
            default -> throw new IllegalArgumentException("Unknown dependency: " + name);
        };
    }

    public record DependencyConfig(
            int maxConnTotal,
            int maxConnPerRoute,
            int bulkheadMaxConcurrent,
            Duration bulkheadMaxWait,
            Duration timeout,
            Duration connectTimeout,
            Duration slowCallDuration,
            float failureRateThreshold,
            int retryMaxAttempts,
            boolean retryEnabled
    ) {

		public static DependencyConfig defaults(
				int maxConnTotal,
				int maxConnPerRoute,
				int bulkheadMaxConcurrent,
				Duration timeout,
				Duration slowCallDuration,
				boolean retryEnabled
		) {
			return new DependencyConfig(
					maxConnTotal,
					maxConnPerRoute,
					bulkheadMaxConcurrent,
					Duration.ZERO,
					timeout,
					Duration.ofSeconds(2),
					slowCallDuration,
					50f,
					2,
					retryEnabled
			);
		}

        public DependencyConfig {
            if (maxConnTotal < 1) {
                maxConnTotal = 4;
            }
            if (maxConnPerRoute < 1) {
                maxConnPerRoute = Math.min(maxConnTotal, 2);
            }
            if (bulkheadMaxConcurrent < 1) {
                bulkheadMaxConcurrent = 2;
            }
            if (bulkheadMaxWait == null) {
                bulkheadMaxWait = Duration.ZERO;
            }
            if (timeout == null) {
                timeout = Duration.ofSeconds(3);
            }
            if (connectTimeout == null) {
                connectTimeout = Duration.ofSeconds(2);
            }
            if (slowCallDuration == null) {
                slowCallDuration = Duration.ofSeconds(2);
            }
            if (failureRateThreshold <= 0 || failureRateThreshold > 100) {
                failureRateThreshold = 50f;
            }
            if (retryMaxAttempts < 1) {
                retryMaxAttempts = 2;
            }
        }
    }

    /** Convenience for tests / Spring binding of nested maps if needed later. */
    public Map<String, DependencyConfig> asMap() {
        return Map.of(
                "rail", rail,
                "finledger", finledger,
                "risk", risk
        );
    }
}
