package com.payhub.notification.infrastructure.webhook;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HmacSignatures {

    public static final String HEADER = "X-PayHub-Signature";

    private HmacSignatures() {
    }

    public static String sha256HeaderValue(String secret, byte[] body) {
        Objects.requireNonNull(secret, "secret");
        Objects.requireNonNull(body, "body");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(body);
            return "sha256=" + HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute HMAC-SHA256", ex);
        }
    }
}
