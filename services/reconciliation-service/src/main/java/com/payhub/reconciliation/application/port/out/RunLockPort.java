package com.payhub.reconciliation.application.port.out;

import java.util.UUID;

public interface RunLockPort {

    /** Acquires a transaction-scoped advisory lock for tenant+rail. */
    void lock(UUID tenantId, String railCode);
}
