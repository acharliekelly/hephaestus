# ADR 0007: Index Spring MVC Endpoints as Deterministic Facts

**Status:** Accepted

**Date:** 2026-07-07

---

## Context

Hephaestus needs to answer which REST endpoints exist in an imported Java project.

Spring MVC endpoint declarations are deterministic source-code facts. They can be extracted from controller and request mapping annotations during static analysis and should not require AI inference.

## Decision

Hephaestus will parse Spring MVC controller annotations with JavaParser and persist endpoint facts in relational storage.

The first supported annotations are:

- `@RestController`
- `@Controller`
- `@RequestMapping`
- `@GetMapping`
- `@PostMapping`
- `@PutMapping`
- `@PatchMapping`
- `@DeleteMapping`

Endpoint APIs will return stored facts with source evidence. The first slice will support direct annotation values such as `"/api/files"` and simple arrays such as `{"/a", "/b"}`. Non-literal or computed paths are out of scope for this slice.

## Consequences

- Endpoint discovery is deterministic and testable.
- Later AI features can use endpoint facts as factual context.
- Unsupported dynamic path expressions will be skipped until a later ADR or implementation slice.
- Changing endpoint discovery to rely on AI requires a future ADR.
