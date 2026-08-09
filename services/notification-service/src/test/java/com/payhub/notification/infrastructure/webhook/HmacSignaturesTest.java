package com.payhub.notification.infrastructure.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class HmacSignaturesTest {

    @Test
    void should_produce_stable_sha256_header() {
        byte[] body = "{\"status\":\"SETTLED\"}".getBytes(StandardCharsets.UTF_8);
        String first = HmacSignatures.sha256HeaderValue("test-secret", body);
        String second = HmacSignatures.sha256HeaderValue("test-secret", body);

        assertThat(first).isEqualTo(second);
        assertThat(first).startsWith("sha256=");
        assertThat(first.length()).isGreaterThan("sha256=".length() + 32);
    }
}
