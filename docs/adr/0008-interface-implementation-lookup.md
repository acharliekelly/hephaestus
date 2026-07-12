# ADR 0008: Query Interface Implementations as Deterministic Graph Facts

**Status:** Accepted

**Date:** 2026-07-12

---

## Context

Hephaestus needs to answer where a Java interface is implemented.

The existing JavaParser indexing flow already records `implements` relationships as deterministic dependency facts. When the target interface symbol exists in the imported repository, `IndexingService` resolves the dependency target to the persisted interface symbol.

## Decision

Hephaestus will expose interface implementation lookup as a deterministic graph query over persisted dependency facts.

The first API will return implementation symbols for a selected interface symbol by finding `DependencyKind.IMPLEMENTS` records whose target symbol is the requested interface symbol.

The API will return the existing symbol response shape, including source path and line number evidence.

## Consequences

- Implementation lookup remains deterministic and testable.
- No new persistence table is needed for this slice.
- Ambiguous or unresolved external interfaces remain outside the result set until a later static-analysis slice improves type resolution.
- Later AI features can use implementation lookup as factual context.
- Changing implementation lookup to rely on AI requires a future ADR.
