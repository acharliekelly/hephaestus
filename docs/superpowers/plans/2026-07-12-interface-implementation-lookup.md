# Interface Implementation Lookup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add deterministic interface implementation lookup so Hephaestus can answer which classes implement a selected Java interface.

**Architecture:** Reuse the existing JavaParser indexing facts: `DependencyKind.IMPLEMENTS` dependencies already connect implementing class symbols to interface symbols when the target interface can be resolved in the imported repository. Add a graph query over those persisted dependency facts and expose it through a small REST API. No new table, parser engine, AI behavior, embedding flow, or external dependency is introduced.

**Tech Stack:** Java 21 target, Spring Boot 3.3.5, Maven wrapper, JavaParser 3.26.2, Spring Data JPA, H2, JUnit 5, AssertJ, MockMvc.

## Global Constraints

- Follow ADR rules in `AGENTS.md`; this slice adds a public API, so add an ADR before implementation.
- Do not change `pom.xml` `<java.version>21</java.version>`.
- Do not change the Maven wrapper or Spring Boot version.
- Keep package root `me.acharliekelly.hephaestus`.
- Keep the app a modular monolith.
- Keep JavaParser as the deterministic static analysis engine.
- Use existing JPA + H2 persistence for this slice; do not add a new table.
- Facts must come from deterministic code analysis, not AI.
- Return existing symbol evidence: source file path and line number.
- Run `./mvnw test` before completion.

---

## File Structure

Create:
- `docs/adr/0008-interface-implementation-lookup.md`: accepted ADR for querying interface implementations from deterministic dependency facts.
- `src/test/java/me/acharliekelly/hephaestus/graph/ImplementationQueryServiceTest.java`: service-level tests for implementation lookup and missing symbol handling.

Modify:
- `src/main/java/me/acharliekelly/hephaestus/model/persistence/DependencyRecordRepository.java`: add a query for dependencies by target symbol and dependency kind.
- `src/main/java/me/acharliekelly/hephaestus/graph/DependencyQueryService.java`: add `implementationsOf(Long interfaceSymbolId)`.
- `src/main/java/me/acharliekelly/hephaestus/web/GraphController.java`: expose `GET /api/symbols/{symbolId}/implementations`.
- `src/test/java/me/acharliekelly/hephaestus/web/ApiIntegrationTest.java`: add MockMvc coverage for the new endpoint.
- `README.md`: document the new interface implementation lookup command.

No new persistence entity, parser DTO, endpoint indexing behavior, or response DTO is introduced in this slice.

---

### Task 1: ADR For Interface Implementation Lookup

**Files:**
- Create: `docs/adr/0008-interface-implementation-lookup.md`

**Interfaces:**
- Produces: accepted architecture decision that implementation lookup is a deterministic graph query over existing dependency facts.
- Consumes: ADR 0004 JavaParser static analysis, ADR 0005 relational persistence, ADR 0006 deterministic output APIs, ADR 0007 endpoint indexing.

- [ ] **Step 1: Create the local branch**

Run:

```bash
git switch -c feature/interface-implementation-lookup
```

Expected: branch changes to `feature/interface-implementation-lookup`.

- [ ] **Step 2: Add the ADR**

Create `docs/adr/0008-interface-implementation-lookup.md`:

```markdown
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
```

- [ ] **Step 3: Commit**

Run:

```bash
git add docs/adr/0008-interface-implementation-lookup.md
git commit -m "Add interface implementation lookup ADR"
```

Expected: commit succeeds with only the ADR.

---

### Task 2: Graph Query For Implementations

**Files:**
- Create: `src/test/java/me/acharliekelly/hephaestus/graph/ImplementationQueryServiceTest.java`
- Modify: `src/main/java/me/acharliekelly/hephaestus/model/persistence/DependencyRecordRepository.java`
- Modify: `src/main/java/me/acharliekelly/hephaestus/graph/DependencyQueryService.java`

**Interfaces:**
- Consumes: `DependencyRecordRepository.findByTargetSymbolId(Long symbolId)`, `DependencyKind.IMPLEMENTS`, `DependencyRecord.getFromSymbol()`.
- Produces: `DependencyRecordRepository.findByTargetSymbolIdAndKind(Long symbolId, DependencyKind kind)`.
- Produces: `DependencyQueryService.implementationsOf(Long interfaceSymbolId): List<CodeSymbolRecord>`.

- [ ] **Step 1: Add failing graph service tests**

Create `src/test/java/me/acharliekelly/hephaestus/graph/ImplementationQueryServiceTest.java`:

```java
package me.acharliekelly.hephaestus.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import me.acharliekelly.hephaestus.indexing.IndexingService;
import me.acharliekelly.hephaestus.model.CodeSymbolRecord;
import me.acharliekelly.hephaestus.model.RepositoryRecord;
import me.acharliekelly.hephaestus.model.persistence.CodeSymbolRecordRepository;
import me.acharliekelly.hephaestus.repo.NotFoundException;
import me.acharliekelly.hephaestus.repo.RepoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ImplementationQueryServiceTest {
    @Autowired
    private RepoService repoService;

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private DependencyQueryService dependencyQueryService;

    @Autowired
    private CodeSymbolRecordRepository symbols;

    @Test
    void findsClassesThatImplementInterface(@TempDir Path projectRoot) throws Exception {
        writeInterfaceFixture(projectRoot);
        RepositoryRecord repository = repoService.importLocal(projectRoot.toString());
        indexingService.indexRepository(repository.getId());

        CodeSymbolRecord paymentPort = onlySymbol(repository.getId(), "PaymentPort");

        assertThat(dependencyQueryService.implementationsOf(paymentPort.getId()))
                .extracting(CodeSymbolRecord::getQualifiedName)
                .containsExactly(
                        "com.example.CardPaymentService",
                        "com.example.WirePaymentService"
                );
    }

    @Test
    void returnsEmptyListForConcreteClassSymbol(@TempDir Path projectRoot) throws Exception {
        writeInterfaceFixture(projectRoot);
        RepositoryRecord repository = repoService.importLocal(projectRoot.toString());
        indexingService.indexRepository(repository.getId());

        CodeSymbolRecord cardPaymentService = onlySymbol(repository.getId(), "CardPaymentService");

        assertThat(dependencyQueryService.implementationsOf(cardPaymentService.getId()))
                .isEmpty();
    }

    @Test
    void rejectsMissingSymbol() {
        assertThatThrownBy(() -> dependencyQueryService.implementationsOf(999_999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Symbol 999999 was not found");
    }

    private CodeSymbolRecord onlySymbol(Long repositoryId, String name) {
        List<CodeSymbolRecord> matches = symbols.findByRepositoryIdAndNameContainingIgnoreCase(repositoryId, name);
        assertThat(matches).hasSize(1);
        return matches.getFirst();
    }

    private void writeInterfaceFixture(Path projectRoot) throws Exception {
        Path packageDir = Files.createDirectories(projectRoot.resolve("src/main/java/com/example"));
        Files.writeString(packageDir.resolve("PaymentPort.java"), """
                package com.example;

                public interface PaymentPort {
                    void charge();
                }
                """);
        Files.writeString(packageDir.resolve("CardPaymentService.java"), """
                package com.example;

                public class CardPaymentService implements PaymentPort {
                    public void charge() {
                    }
                }
                """);
        Files.writeString(packageDir.resolve("WirePaymentService.java"), """
                package com.example;

                public class WirePaymentService implements PaymentPort {
                    public void charge() {
                    }
                }
                """);
    }
}
```

- [ ] **Step 2: Verify red**

Run:

```bash
./mvnw test -Dtest=ImplementationQueryServiceTest
```

Expected: FAIL at test compile because `DependencyQueryService.implementationsOf(Long)` does not exist.

- [ ] **Step 3: Add repository query**

Modify `src/main/java/me/acharliekelly/hephaestus/model/persistence/DependencyRecordRepository.java` to import `DependencyKind` and add the new method after `findByTargetSymbolId`:

```java
package me.acharliekelly.hephaestus.model.persistence;

import java.util.List;
import me.acharliekelly.hephaestus.model.DependencyKind;
import me.acharliekelly.hephaestus.model.DependencyRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DependencyRecordRepository extends JpaRepository<DependencyRecord, Long> {
    @EntityGraph(attributePaths = {"sourceFile", "fromSymbol", "targetSymbol"})
    List<DependencyRecord> findByFromSymbolId(Long symbolId);

    @EntityGraph(attributePaths = {"sourceFile", "fromSymbol", "targetSymbol"})
    List<DependencyRecord> findByTargetSymbolId(Long symbolId);

    @EntityGraph(attributePaths = {"sourceFile", "fromSymbol", "fromSymbol.sourceFile", "targetSymbol", "targetSymbol.sourceFile"})
    List<DependencyRecord> findByTargetSymbolIdAndKind(Long symbolId, DependencyKind kind);

    @EntityGraph(attributePaths = {"sourceFile", "fromSymbol", "fromSymbol.sourceFile", "targetSymbol", "targetSymbol.sourceFile"})
    List<DependencyRecord> findByRepositoryId(Long repositoryId);

    @Modifying
    @Query("delete from DependencyRecord dependency where dependency.repository.id = :repositoryId")
    void deleteByRepositoryId(Long repositoryId);
}
```

- [ ] **Step 4: Add graph service method**

Modify `src/main/java/me/acharliekelly/hephaestus/graph/DependencyQueryService.java` to import `Comparator`, `Objects`, and `DependencyKind`, then add `implementationsOf` after `dependentsOf`:

```java
package me.acharliekelly.hephaestus.graph;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import me.acharliekelly.hephaestus.model.CodeSymbolRecord;
import me.acharliekelly.hephaestus.model.DependencyKind;
import me.acharliekelly.hephaestus.model.DependencyRecord;
import me.acharliekelly.hephaestus.model.persistence.CodeSymbolRecordRepository;
import me.acharliekelly.hephaestus.model.persistence.DependencyRecordRepository;
import me.acharliekelly.hephaestus.repo.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DependencyQueryService {
    private final CodeSymbolRecordRepository symbols;
    private final DependencyRecordRepository dependencies;

    public DependencyQueryService(
            CodeSymbolRecordRepository symbols,
            DependencyRecordRepository dependencies
    ) {
        this.symbols = symbols;
        this.dependencies = dependencies;
    }

    @Transactional(readOnly = true)
    public List<CodeSymbolRecord> findSymbols(Long repositoryId, String name) {
        String query = name == null ? "" : name;
        return symbols.findByRepositoryIdAndNameContainingIgnoreCase(repositoryId, query);
    }

    @Transactional(readOnly = true)
    public List<DependencyRecord> dependenciesOf(Long symbolId) {
        requireSymbol(symbolId);
        return dependencies.findByFromSymbolId(symbolId);
    }

    @Transactional(readOnly = true)
    public List<DependencyRecord> dependentsOf(Long symbolId) {
        requireSymbol(symbolId);
        return dependencies.findByTargetSymbolId(symbolId);
    }

    @Transactional(readOnly = true)
    public List<CodeSymbolRecord> implementationsOf(Long interfaceSymbolId) {
        requireSymbol(interfaceSymbolId);
        return dependencies.findByTargetSymbolIdAndKind(interfaceSymbolId, DependencyKind.IMPLEMENTS).stream()
                .map(DependencyRecord::getFromSymbol)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(CodeSymbolRecord::getQualifiedName))
                .toList();
    }

    private void requireSymbol(Long symbolId) {
        if (!symbols.existsById(symbolId)) {
            throw new NotFoundException("Symbol " + symbolId + " was not found");
        }
    }
}
```

- [ ] **Step 5: Verify graph tests pass**

Run:

```bash
./mvnw test -Dtest=ImplementationQueryServiceTest
```

Expected: PASS with 3 tests, 0 failures.

- [ ] **Step 6: Run existing dependency API test to catch regressions**

Run:

```bash
./mvnw test -Dtest=ApiIntegrationTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```bash
git add src/test/java/me/acharliekelly/hephaestus/graph/ImplementationQueryServiceTest.java \
  src/main/java/me/acharliekelly/hephaestus/model/persistence/DependencyRecordRepository.java \
  src/main/java/me/acharliekelly/hephaestus/graph/DependencyQueryService.java
git commit -m "Add deterministic implementation lookup query"
```

Expected: commit succeeds with the graph query and service tests.

---

### Task 3: REST API For Interface Implementations

**Files:**
- Modify: `src/main/java/me/acharliekelly/hephaestus/web/GraphController.java`
- Modify: `src/test/java/me/acharliekelly/hephaestus/web/ApiIntegrationTest.java`

**Interfaces:**
- Consumes: `DependencyQueryService.implementationsOf(Long interfaceSymbolId): List<CodeSymbolRecord>`.
- Produces: `GET /api/symbols/{symbolId}/implementations`.
- Produces response body: `List<SymbolResponse>`.

- [ ] **Step 1: Add failing API regression test**

Append this test to `src/test/java/me/acharliekelly/hephaestus/web/ApiIntegrationTest.java` before `indexingPersistenceFailuresReturnUnprocessableEntity`:

```java
    @Test
    void queriesInterfaceImplementations(@TempDir Path projectRoot) throws Exception {
        writeFixture(projectRoot);

        String importJson = mockMvc.perform(post("/api/repositories/import-local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ImportLocalRepositoryRequest(projectRoot.toString()))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long repositoryId = objectMapper.readValue(importJson, RepositoryResponse.class).id();

        mockMvc.perform(post("/api/repositories/{repositoryId}/index", repositoryId))
                .andExpect(status().isOk());

        String portJson = mockMvc.perform(get("/api/repositories/{repositoryId}/symbols", repositoryId)
                        .param("name", "PaymentPort"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<SymbolResponse> ports = objectMapper.readValue(portJson, new TypeReference<>() {
        });
        Long portId = ports.getFirst().id();

        String implementationsJson = mockMvc.perform(get("/api/symbols/{symbolId}/implementations", portId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<SymbolResponse> implementations = objectMapper.readValue(implementationsJson, new TypeReference<>() {
        });
        assertThat(implementations)
                .extracting(SymbolResponse::qualifiedName)
                .containsExactly("com.example.PaymentService");
        assertThat(implementations.getFirst().sourcePath()).endsWith("PaymentService.java");
    }
```

- [ ] **Step 2: Verify red**

Run:

```bash
./mvnw test -Dtest=ApiIntegrationTest
```

Expected: FAIL with HTTP 404 or no handler for `/api/symbols/{symbolId}/implementations`.

- [ ] **Step 3: Add controller endpoint**

Modify `src/main/java/me/acharliekelly/hephaestus/web/GraphController.java` to add this method after `dependents`:

```java
    @GetMapping("/symbols/{symbolId}/implementations")
    public List<SymbolResponse> implementations(@PathVariable Long symbolId) {
        return dependencyQueryService.implementationsOf(symbolId).stream()
                .map(SymbolResponse::from)
                .toList();
    }
```

The final controller should be:

```java
package me.acharliekelly.hephaestus.web;

import java.util.List;
import me.acharliekelly.hephaestus.graph.DependencyQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GraphController {
    private final DependencyQueryService dependencyQueryService;

    public GraphController(DependencyQueryService dependencyQueryService) {
        this.dependencyQueryService = dependencyQueryService;
    }

    @GetMapping("/repositories/{repositoryId}/symbols")
    public List<SymbolResponse> symbols(
            @PathVariable Long repositoryId,
            @RequestParam(defaultValue = "") String name
    ) {
        return dependencyQueryService.findSymbols(repositoryId, name).stream()
                .map(SymbolResponse::from)
                .toList();
    }

    @GetMapping("/symbols/{symbolId}/dependencies")
    public List<DependencyResponse> dependencies(@PathVariable Long symbolId) {
        return dependencyQueryService.dependenciesOf(symbolId).stream()
                .map(DependencyResponse::from)
                .toList();
    }

    @GetMapping("/symbols/{symbolId}/dependents")
    public List<DependencyResponse> dependents(@PathVariable Long symbolId) {
        return dependencyQueryService.dependentsOf(symbolId).stream()
                .map(DependencyResponse::from)
                .toList();
    }

    @GetMapping("/symbols/{symbolId}/implementations")
    public List<SymbolResponse> implementations(@PathVariable Long symbolId) {
        return dependencyQueryService.implementationsOf(symbolId).stream()
                .map(SymbolResponse::from)
                .toList();
    }
}
```

- [ ] **Step 4: Verify API test passes**

Run:

```bash
./mvnw test -Dtest=ApiIntegrationTest
```

Expected: PASS, including `queriesInterfaceImplementations`.

- [ ] **Step 5: Verify missing symbol still returns 404 through existing handler**

Run:

```bash
./mvnw test -Dtest=ImplementationQueryServiceTest,ApiIntegrationTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```bash
git add src/main/java/me/acharliekelly/hephaestus/web/GraphController.java \
  src/test/java/me/acharliekelly/hephaestus/web/ApiIntegrationTest.java
git commit -m "Expose interface implementation lookup API"
```

Expected: commit succeeds with controller and API test changes.

---

### Task 4: README And Full Verification

**Files:**
- Modify: `README.md`

**Interfaces:**
- Documents: `GET /api/symbols/{symbolId}/implementations`.
- Verifies: full Maven test suite and a manual HTTP smoke test.

- [ ] **Step 1: Update README API docs**

In `README.md`, replace the current dependency query section with this content:

````markdown
### Query Dependencies And Implementations

```bash
curl http://localhost:8080/api/symbols/{symbolId}/dependencies
curl http://localhost:8080/api/symbols/{symbolId}/dependents
curl http://localhost:8080/api/symbols/{symbolId}/implementations
```
````

- [ ] **Step 2: Run full verification**

Run:

```bash
./mvnw test
```

Expected: BUILD SUCCESS with all tests passing.

- [ ] **Step 3: Manual smoke test fixture**

Create this temporary fixture outside the repo:

```bash
mkdir -p /tmp/hephaestus-implementations-smoke/src/main/java/com/example
cat > /tmp/hephaestus-implementations-smoke/src/main/java/com/example/PaymentPort.java <<'JAVA'
package com.example;

public interface PaymentPort {
    void charge();
}
JAVA
cat > /tmp/hephaestus-implementations-smoke/src/main/java/com/example/PaymentService.java <<'JAVA'
package com.example;

public class PaymentService implements PaymentPort {
    public void charge() {
    }
}
JAVA
```

- [ ] **Step 4: Start the app**

Run the app on an alternate port if 8080 is occupied:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=18080
```

Expected: app starts and reports `Tomcat started on port 18080`.

- [ ] **Step 5: Run smoke API calls**

In another terminal, run:

```bash
curl -s -X POST http://localhost:18080/api/repositories/import-local \
  -H 'Content-Type: application/json' \
  -d '{"path":"/tmp/hephaestus-implementations-smoke"}'
curl -s -X POST http://localhost:18080/api/repositories/1/index
curl -s 'http://localhost:18080/api/repositories/1/symbols?name=PaymentPort'
curl -s http://localhost:18080/api/symbols/1/implementations
```

Expected:
- Import response contains `"id":1`.
- Index response contains `"sourceFileCount":2`.
- Symbol response contains `"qualifiedName":"com.example.PaymentPort"`.
- Implementations response contains `"qualifiedName":"com.example.PaymentService"`.

Stop the app with `Ctrl-C` after the smoke test.

- [ ] **Step 6: Commit**

Run:

```bash
git add README.md
git commit -m "Document interface implementation lookup API"
```

Expected: commit succeeds with README only.

---

## Self-Review

Spec coverage:
- The plan answers the MVP question “Where is this interface implemented?” through a deterministic graph API.
- The plan includes an ADR because a public API is introduced.
- The implementation uses existing `DependencyKind.IMPLEMENTS` persistence facts and does not add a table.
- The plan returns existing `SymbolResponse` evidence: source path and line number.
- The plan avoids AI, embeddings, parser-engine changes, and external services.

Placeholder scan:
- No TBD/TODO/fill-in placeholders remain.
- Every code-changing step includes concrete code or exact replacement text.
- Every verification step names the command and expected result.

Type consistency:
- `DependencyRecordRepository.findByTargetSymbolIdAndKind(Long, DependencyKind)` is defined before `DependencyQueryService` consumes it.
- `DependencyQueryService.implementationsOf(Long)` is defined before `GraphController` consumes it.
- The REST endpoint returns `List<SymbolResponse>`, matching existing symbol response mapping and test deserialization.
