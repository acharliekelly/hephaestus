# ADR 0002: Use Spring Boot and Maven for the Application Foundation

**Status:** Accepted

**Date:** 2026-07-02

---

## Context

Hephaestus needs a Java application foundation for REST APIs, persistence, validation, testing, and later Spring AI integration.

The project should remain easy to run locally and should not depend on a globally installed Maven binary.

## Decision

Hephaestus will begin as a Java 21 Spring Boot application built with Maven.

The repository will include a Maven wrapper script so project commands can be run as `./mvnw ...`.

## Consequences

- Spring Boot provides a conventional path for web APIs, JPA persistence, validation, and tests.
- Maven keeps dependency management explicit and familiar for Java application contributors.
- The wrapper improves local reproducibility.
- Build configuration changes should preserve Java 21 compatibility unless superseded by a later ADR.

