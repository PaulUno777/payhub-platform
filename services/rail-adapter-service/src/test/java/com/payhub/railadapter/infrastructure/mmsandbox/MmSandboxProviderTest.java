package com.payhub.railadapter.infrastructure.mmsandbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.payhub.railadapter.application.port.out.RailProviderPort.ProofCommand;
import com.payhub.railadapter.application.port.out.RailProviderPort.SubmitCommand;
import com.payhub.railadapter.domain.RailOutcome;
import com.payhub.railadapter.domain.SandboxMode;

@Tag("unit")
class MmSandboxProviderTest {

    private final MmSandboxProvider provider = new MmSandboxProvider();

    @Test
    void should_accept_submit_in_accept_mode() {
        UUID paymentId = UUID.randomUUID();
        var result = provider.submit(new SubmitCommand(paymentId, "10.00", "USD", SandboxMode.ACCEPT));
        assertThat(result.outcome()).isEqualTo(RailOutcome.ACCEPTED);
        assertThat(result.providerReference()).isEqualTo("mm-" + paymentId);
    }

    @Test
    void should_reject_submit_in_reject_mode() {
        var result = provider.submit(new SubmitCommand(UUID.randomUUID(), "10.00", "USD", SandboxMode.REJECT));
        assertThat(result.outcome()).isEqualTo(RailOutcome.REJECTED);
        assertThat(result.providerReference()).isNull();
    }

    @Test
    void should_return_ambiguous_on_submit() {
        var result = provider.submit(new SubmitCommand(UUID.randomUUID(), "10.00", "USD", SandboxMode.AMBIGUOUS));
        assertThat(result.outcome()).isEqualTo(RailOutcome.AMBIGUOUS);
    }

    @Test
    void should_return_ambiguous_on_proof_when_proof_ambiguous_mode() {
        var result = provider.awaitProof(new ProofCommand(UUID.randomUUID(), "mm-1", SandboxMode.PROOF_AMBIGUOUS));
        assertThat(result.outcome()).isEqualTo(RailOutcome.AMBIGUOUS);
    }

    @Test
    void should_accept_proof_in_accept_mode() {
        var result = provider.awaitProof(new ProofCommand(UUID.randomUUID(), "mm-1", SandboxMode.ACCEPT));
        assertThat(result.outcome()).isEqualTo(RailOutcome.ACCEPTED);
    }
}
