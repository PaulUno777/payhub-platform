package com.payhub.railadapter.application.usecase;

import org.springframework.stereotype.Service;

import com.payhub.railadapter.application.port.in.SubmitRailOperationUseCase;
import com.payhub.railadapter.application.port.out.RailProviderPort;
import com.payhub.railadapter.application.port.out.RailProviderPort.SubmitCommand;

@Service
public class SubmitRailOperationService implements SubmitRailOperationUseCase {

    private final RailProviderPort railProviderPort;

    public SubmitRailOperationService(RailProviderPort railProviderPort) {
        this.railProviderPort = railProviderPort;
    }

    @Override
    public Result execute(Command command) {
        var result = railProviderPort.submit(new SubmitCommand(
                command.paymentId(),
                command.amount(),
                command.currencyCode(),
                SandboxModes.parse(command.sandboxMode())
        ));
        return new Result(result.outcome().name(), result.providerReference());
    }
}
