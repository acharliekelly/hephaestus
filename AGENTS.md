# Agent Instructions

## Architecture Decisions

This project uses Architecture Decision Records in `docs/adr/`.

If a requested change affects an accepted architectural decision, do not silently implement it.

Instead:

1. Identify the relevant ADR.
2. Explain the conflict.
3. Propose either:
   - a new ADR, or
   - an update to the existing ADR.
4. Wait for human approval before changing the architecture.

Examples of architecture-impacting changes include:

- replacing JavaParser
- changing the persistence strategy
- introducing a graph database
- splitting the application into microservices
- changing the AI orchestration approach
- adding a new external service dependency

Create or update an ADR only for decisions that materially affect the project's architecture, technology stack, module boundaries, persistence strategy, AI orchestration, or public APIs. Routine implementation details do not require ADRs.