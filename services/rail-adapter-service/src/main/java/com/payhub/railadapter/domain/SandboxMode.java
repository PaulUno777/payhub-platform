package com.payhub.railadapter.domain;

/**
 * MmSandbox behaviour knobs for DS-008 exit cases.
 */
public enum SandboxMode {
    ACCEPT,
    REJECT,
    AMBIGUOUS,
    /** Submit accepts; final proof is ambiguous (timeout after PENDING). */
    PROOF_AMBIGUOUS
}
