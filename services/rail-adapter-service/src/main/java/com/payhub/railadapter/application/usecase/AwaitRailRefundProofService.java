package com.payhub.railadapter.application.usecase;

import org.springframework.stereotype.Service;

import com.payhub.railadapter.application.port.in.AwaitRailRefundProofUseCase;
import com.payhub.railadapter.application.port.out.RailProviderPort;
import com.payhub.railadapter.application.port.out.RailProviderPort.ProofCommand;

@Service
public class AwaitRailRefundProofService implements AwaitRailRefundProofUseCase {

    private final RailProviderPort railProviderPort;

    public AwaitRailRefundProofService(RailProviderPort railProviderPort) {
        this.railProviderPort = railProviderPort;
    }

    @Override
    public Result execute(Command command) {
        var result = railProviderPort.awaitRefundProof(new ProofCommand(
                command.paymentId(),
                command.providerReference(),
                SandboxModes.parse(command.sandboxMode())
        ));
        return new Result(result.outcome().name());
    }
}
