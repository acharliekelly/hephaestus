# ADR 0001: Use Architecture Decision Records

**Status:** Accepted

**Date:** 2026-07-02

---

## Context

Hephaestus is intended to be a long-lived project that will evolve through experimentation with AI, static analysis, and developer tooling.

Many technical decisions made early in the project are expected to be revisited over time. Without a documented rationale, future contributors (human or AI) may be unable to distinguish intentional design decisions from accidental implementation details.

Additionally, AI coding agents often optimize for the current task without awareness of historical architectural decisions unless those decisions are explicitly documented.

---

## Decision

The project will maintain Architecture Decision Records (ADRs) under `docs/adr/`.

Each ADR documents a single significant architectural decision, including:

- the problem being addressed
- the decision that was made
- the reasoning behind the decision
- the consequences and tradeoffs

ADRs are permanent records of architectural decisions at a point in time.

If a decision changes, a new ADR should normally supersede the earlier one rather than rewriting history.

---

## Consequences

### Positive

- Architectural reasoning is preserved.
- New contributors can understand *why* decisions were made.
- AI agents have explicit architectural context.
- Revisiting old decisions becomes easier.
- Major design changes become intentional and reviewable.

### Negative

- Significant architectural changes require additional documentation.
- Contributors must decide whether a change warrants a new ADR.

---

## Agent Guidance

When implementing changes:

- Follow accepted ADRs.
- If a requested feature conflicts with an accepted ADR, do not silently change the implementation.
- Instead, propose a new ADR or an update for human review before proceeding.

---

## Related

None.