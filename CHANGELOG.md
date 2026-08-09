# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- DS-005: FinLedger outbox → Debezium → Kafka (`ledger.journal-entry.v1`), Schema Registry,
  AsyncAPI, reporting-service inbox consumer (duplicate delivery = one projection)
- DS-004: Merchant aggregate (`PENDING_REVIEW` → `ACTIVE`/`REJECTED`), FinLedger
  `AccountProvisioningPort` (`SUB_MERCHANT` + wallets), Ops BFF approve/reject/get proxies
- DS-003: Spring Cloud Config Server, standardized ports, FinLedger Compose + Orchestrator
  `LedgerPort` / `FinLedgerClient` with idempotent replay contract test
- DS-002 repo foundation: ten hexagonal Spring Boot 4.1.0 services, ArchUnit suites,
  Maven reactor, Compose Postgres stubs, CI workflow, contract stubs
- DS-001 documentation bootstrap: context map, event catalog, CAP/PACELC ADR,
  FinLedger pin (`unoteck/finledger:0.1.0`)
