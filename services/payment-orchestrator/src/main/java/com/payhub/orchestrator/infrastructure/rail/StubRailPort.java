package com.payhub.orchestrator.infrastructure.rail;

import org.springframework.stereotype.Component;

import com.payhub.orchestrator.application.port.out.RailPort;

@Component
public class StubRailPort implements RailPort {

    @Override
    public RailResult submit(RailSubmitCommand command) {
        throw new UnsupportedOperationException("RailPort submit is DS-008; not used on DS-006 sync path");
    }
}
