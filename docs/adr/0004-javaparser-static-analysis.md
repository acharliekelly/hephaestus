# ADR 0004: Use JavaParser for Initial Static Analysis

**Status:** Accepted

**Date:** 2026-07-02

---

## Context

The core Hephaestus principle is that facts come from deterministic code analysis, not from an LLM.

The first implementation needs to parse Java source files and extract packages, symbols, imports, annotations, inheritance, implemented interfaces, fields, methods, and basic method calls.

## Decision

Hephaestus will use JavaParser as the initial Java static analysis engine.

AI components must not infer deterministic architecture relationships that JavaParser or later static analysis components can discover directly.

## Consequences

- Static facts are reproducible and testable.
- JavaParser gives the first slice a focused parser dependency without requiring compiler integration.
- Some advanced facts may require later symbol solving or compiler-backed analysis.
- Replacing JavaParser requires a future ADR.

