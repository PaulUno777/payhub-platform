package com.payhub.railadapter.application.usecase;

import com.payhub.railadapter.domain.SandboxMode;

final class SandboxModes {

    private SandboxModes() {
    }

    static SandboxMode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return SandboxMode.ACCEPT;
        }
        return SandboxMode.valueOf(raw.trim().toUpperCase());
    }
}
