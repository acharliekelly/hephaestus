# ADR 0005: Use Relational Persistence with H2 Initially

**Status:** Accepted

**Date:** 2026-07-02

---

## Context

Hephaestus needs to store deterministic architecture facts and query dependencies, dependents, source files, and symbols.

The project overview allows relational storage initially and defers graph databases for later evaluation.

## Decision

Hephaestus will use Spring Data JPA with H2 as the initial relational persistence strategy.

The architecture graph will be represented as relational entities for repositories, source files, code symbols, and dependencies.

## Consequences

- The first implementation can run and test without an external database.
- The data model remains explicit and inspectable.
- Query needs that exceed relational storage can be evaluated later.
- Introducing PostgreSQL, Neo4j, or another persistence strategy requires a future ADR.

