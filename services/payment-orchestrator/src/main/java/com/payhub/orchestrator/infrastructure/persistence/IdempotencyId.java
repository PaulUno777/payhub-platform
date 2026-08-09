package com.payhub.orchestrator.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;

public class IdempotencyId implements Serializable {

    private String operation;
    private String idempotencyKey;

    public IdempotencyId() {
    }

    public IdempotencyId(String operation, String idempotencyKey) {
        this.operation = operation;
        this.idempotencyKey = idempotencyKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IdempotencyId that)) {
            return false;
        }
        return Objects.equals(operation, that.operation)
                && Objects.equals(idempotencyKey, that.idempotencyKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operation, idempotencyKey);
    }
}
