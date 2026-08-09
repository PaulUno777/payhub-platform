package com.payhub.orchestrator.infrastructure.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.payhub.orchestrator.application.port.out.RailPort.RailResult;
import com.payhub.orchestrator.application.port.out.RailPort.RailSubmitCommand;
import com.payhub.orchestrator.application.port.out.RailPort.RailSubmitResult;
import com.payhub.orchestrator.infrastructure.rail.HttpRailPort;
import com.payhub.orchestrator.infrastructure.resilience.OutboundResilienceConfig.DependencyResilience;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@Tag("unit")
class HttpRailPortBulkheadMappingTest {

    @Test
    void should_map_bulkhead_full_to_ambiguous() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start();
        try {
            server.enqueue(new MockResponse()
                    .setBody("{\"outcome\":\"ACCEPTED\",\"providerReference\":\"p1\"}")
                    .addHeader("Content-Type", "application/json")
                    .setBodyDelay(2, TimeUnit.SECONDS));
            server.enqueue(new MockResponse()
                    .setBody("{\"outcome\":\"ACCEPTED\",\"providerReference\":\"p2\"}")
                    .addHeader("Content-Type", "application/json")
                    .setBodyDelay(2, TimeUnit.SECONDS));

            ResilienceProperties.DependencyConfig railCfg = new ResilienceProperties.DependencyConfig(
                    2, 2, 1, Duration.ZERO, Duration.ofSeconds(5), Duration.ofSeconds(1),
                    Duration.ofSeconds(10), 100f, 1, false
            );
            DependencyResilience resilience = TestDependencyResilience.create(
                    railCfg,
                    ResilienceProperties.DependencyConfig.defaults(8, 4, 4, Duration.ofSeconds(2), Duration.ofSeconds(2), false),
                    ResilienceProperties.DependencyConfig.defaults(4, 2, 2, Duration.ofSeconds(1), Duration.ofSeconds(1), false)
            );
            HttpRailPort railPort = new HttpRailPort(
                    OutboundResilienceConfig.buildClient(railCfg, server.url("/").toString().replaceAll("/$", "")),
                    resilience
            );

            CountDownLatch holding = new CountDownLatch(1);
            CountDownLatch finished = new CountDownLatch(1);
            AtomicReference<RailSubmitResult> second = new AtomicReference<>();

            Thread holder = new Thread(() -> {
                holding.countDown();
                railPort.submit(new RailSubmitCommand(UUID.randomUUID(), "1.00", "XOF", "ACCEPT"));
                finished.countDown();
            });
            holder.start();
            assertThat(holding.await(2, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(100);

            second.set(railPort.submit(new RailSubmitCommand(UUID.randomUUID(), "1.00", "XOF", "ACCEPT")));
            assertThat(second.get().outcome()).isEqualTo(RailResult.AMBIGUOUS);

            assertThat(finished.await(10, TimeUnit.SECONDS)).isTrue();
            holder.join(TimeUnit.SECONDS.toMillis(5));
        } finally {
            server.shutdown();
        }
    }
}
