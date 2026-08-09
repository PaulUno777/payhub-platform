package com.payhub.railadapter.application.usecase;

import org.springframework.stereotype.Service;

import com.payhub.railadapter.application.port.in.AwaitRailProofUseCase;
import com.payhub.railadapter.application.port.out.RailProviderPort;
import com.payhub.railadapter.application.port.out.RailProviderPort.ProofCommand;

@Service
public class AwaitRailProofService implements AwaitRailProofUseCase {

    private final RailProviderPort railProviderPort;

    public AwaitRailProofService(RailProviderPort railProviderPort) {
        this.railProviderPort = railProviderPort;
    }

    @Override
    public Result execute(Command command) {
        var result = railProviderPort.awaitProof(new ProofCommand(
                command.paymentId(),
                command.providerReference(),
                SandboxModes.parse(command.sandboxMode())
        ));
        return new Result(result.outcome().name());
    }
}
