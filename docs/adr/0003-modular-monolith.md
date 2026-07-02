# ADR 0003: Start as a Modular Monolith

**Status:** Accepted

**Date:** 2026-07-02

---

## Context

Hephaestus has several conceptual modules: repository workspace handling, static indexing, architecture graph queries, semantic search, and AI orchestration.

Splitting these capabilities into separate deployable services now would add operational and integration complexity before the domain model is proven.

## Decision

Hephaestus will begin as a modular monolith.

Modules will be separated by package responsibility inside one Spring Boot application. External service boundaries may be introduced later only through a new ADR.

## Consequences

- Early development stays simple and locally testable.
- Module boundaries remain visible in package structure.
- Future extraction remains possible after responsibilities and interfaces stabilize.
- Introducing microservices would conflict with this ADR unless a future ADR supersedes it.

