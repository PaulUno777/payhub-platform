package com.payhub.reconciliation.infrastructure.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.payhub.reconciliation.application.port.out.RunLockPort;

@Component
public class PostgresAdvisoryRunLock implements RunLockPort {

    private final JdbcTemplate jdbcTemplate;

    public PostgresAdvisoryRunLock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void lock(UUID tenantId, String railCode) {
        long key = hash(tenantId, railCode);
        jdbcTemplate.query(
                "select pg_advisory_xact_lock(?)",
                ps -> ps.setLong(1, key),
                rs -> null
        );
    }

    static long hash(UUID tenantId, String railCode) {
        long h = tenantId.getMostSignificantBits() ^ tenantId.getLeastSignificantBits();
        h = 31 * h + (railCode == null ? 0 : railCode.hashCode());
        return h;
    }
}
