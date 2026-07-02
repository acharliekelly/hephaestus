# ADR 0006: Generate Architecture Outputs from Deterministic Facts

**Status:** Accepted

**Date:** 2026-07-02

---

## Context

Hephaestus needs to produce architecture summaries and dependency diagrams for the MVP.

The project principle is that facts come from code and explanations come from AI. Summaries and diagrams describe factual structure, so their source data must be the persisted architecture graph rather than LLM inference.

## Decision

Architecture summary and Mermaid dependency diagram APIs will be generated from stored graph facts.

These APIs may format and aggregate facts, but they must not use AI to discover packages, symbols, dependencies, or relationships.

## Consequences

- Summary and diagram output is deterministic and testable.
- Later AI features can consume these APIs as factual context.
- Diagram rendering remains a client concern; the API returns Mermaid text.
- Changing these APIs to rely on AI for factual structure requires a future ADR.

