package com.payhub.reconciliation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class BreakTest {

    @Test
    void should_open_and_resolve_with_audited_confirm() {
        Break brk = Break.open(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ext-1",
                BreakType.STATUS_MISMATCH,
                "statement SETTLED vs payment RECONCILIATION_REQUIRED"
        );
        assertThat(brk.status()).isEqualTo(BreakStatus.OPEN);

        brk.resolve(ResolveAction.CONFIRM, "ops-alice");

        assertThat(brk.status()).isEqualTo(BreakStatus.RESOLVED);
        assertThat(brk.resolutionAction()).isEqualTo(ResolveAction.CONFIRM);
        assertThat(brk.resolvedBy()).isEqualTo("ops-alice");
        assertThat(brk.resolvedAt()).isNotNull();
    }

    @Test
    void should_resolve_with_request_reversal() {
        Break brk = Break.open(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ext-2",
                BreakType.STATUS_MISMATCH,
                "orphan PENDING"
        );
        brk.resolve(ResolveAction.REQUEST_REVERSAL, "ops-bob");
        assertThat(brk.resolutionAction()).isEqualTo(ResolveAction.REQUEST_REVERSAL);
    }

    @Test
    void should_forbid_resolve_without_actor() {
        Break brk = Break.open(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ext-3",
                BreakType.STATUS_MISMATCH,
                "detail"
        );
        assertThatThrownBy(() -> brk.resolve(ResolveAction.CONFIRM, null))
                .isInstanceOf(NullPointerException.class);
    }
}
