# Spring Endpoint Indexing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add deterministic Spring MVC endpoint indexing and query APIs so Hephaestus can answer which REST endpoints exist in an imported Java project.

**Architecture:** Extend the existing deterministic parser/indexer flow. JavaParser extracts endpoint facts from Spring MVC annotations, `IndexingService` persists them alongside source files and symbols, and a graph/web query service exposes repository endpoint lookup APIs. No AI, Git integration, embeddings, or source modification are included.

**Tech Stack:** Java 21, Spring Boot 3.3.5, Maven wrapper, JavaParser 3.26.2, Spring Data JPA, H2, JUnit 5, AssertJ, MockMvc.

## Global Constraints

- Follow ADR rules in `AGENTS.md`; this slice adds persisted facts and public APIs, so add an ADR before implementation.
- Keep package root `me.acharliekelly.hephaestus`.
- Keep the app a modular monolith.
- Use JavaParser for Java static analysis.
- Use JPA + H2 for this slice.
- Facts must come from deterministic code analysis, not AI.
- Store evidence for endpoint facts: source file path and line number.
- Run `./mvnw test` before completion.

---

## File Structure

Create:
- `docs/adr/0007-spring-endpoint-indexing.md`: accepted ADR for deterministic Spring endpoint indexing.
- `src/main/java/me/acharliekelly/hephaestus/indexing/ParsedEndpoint.java`: parsed endpoint fact emitted by JavaParser.
- `src/main/java/me/acharliekelly/hephaestus/model/HttpMethod.java`: enum for endpoint HTTP methods.
- `src/main/java/me/acharliekelly/hephaestus/model/EndpointRecord.java`: JPA entity for persisted endpoint facts.
- `src/main/java/me/acharliekelly/hephaestus/model/persistence/EndpointRecordRepository.java`: repository queries for endpoints.
- `src/main/java/me/acharliekelly/hephaestus/graph/EndpointQueryService.java`: deterministic endpoint lookup service.
- `src/main/java/me/acharliekelly/hephaestus/web/EndpointResponse.java`: API response DTO.
- `src/main/java/me/acharliekelly/hephaestus/web/EndpointController.java`: REST API for endpoint queries.
- `src/test/java/me/acharliekelly/hephaestus/graph/EndpointQueryServiceTest.java`: persistence/query tests.
- `src/test/java/me/acharliekelly/hephaestus/web/EndpointApiTest.java`: API tests.

Modify:
- `src/main/java/me/acharliekelly/hephaestus/indexing/ParsedSourceFile.java`: include parsed endpoint list.
- `src/main/java/me/acharliekelly/hephaestus/indexing/JavaParserService.java`: extract Spring MVC endpoint annotations.
- `src/main/java/me/acharliekelly/hephaestus/indexing/IndexingService.java`: delete/reinsert endpoint facts during indexing.
- `src/test/java/me/acharliekelly/hephaestus/indexing/JavaParserServiceTest.java`: parser endpoint tests.
- `src/test/java/me/acharliekelly/hephaestus/indexing/IndexingServiceTest.java`: re-index endpoint persistence test.
- `README.md`: add endpoint API command after implementation.

---

### Task 1: ADR And Parsed Endpoint Contract

**Files:**
- Create: `docs/adr/0007-spring-endpoint-indexing.md`
- Create: `src/main/java/me/acharliekelly/hephaestus/indexing/ParsedEndpoint.java`
- Modify: `src/main/java/me/acharliekelly/hephaestus/indexing/ParsedSourceFile.java`

**Interfaces:**
- Produces: `ParsedEndpoint(String httpMethod, String path, String controllerQualifiedName, String handlerQualifiedName, Integer lineNumber)`
- Produces: `ParsedSourceFile(..., List<ParsedEndpoint> endpoints)`

- [ ] **Step 1: Add the ADR**

Create `docs/adr/0007-spring-endpoint-indexing.md`:

```markdown
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
```

- [ ] **Step 2: Add parsed endpoint record**

Create `src/main/java/me/acharliekelly/hephaestus/indexing/ParsedEndpoint.java`:

```java
package me.acharliekelly.hephaestus.indexing;

public record ParsedEndpoint(
        String httpMethod,
        String path,
        String controllerQualifiedName,
        String handlerQualifiedName,
        Integer lineNumber
) {
}
```

- [ ] **Step 3: Extend parsed source file contract**

Modify `src/main/java/me/acharliekelly/hephaestus/indexing/ParsedSourceFile.java`:

```java
package me.acharliekelly.hephaestus.indexing;

import java.util.List;

public record ParsedSourceFile(
        String relativePath,
        String absolutePath,
        String packageName,
        List<ParsedSymbol> symbols,
        List<ParsedDependency> dependencies,
        List<ParsedEndpoint> endpoints
) {
}
```

- [ ] **Step 4: Run compile to expose call sites**

Run: `./mvnw test -Dtest=JavaParserServiceTest`

Expected: compile fails where `ParsedSourceFile` is still constructed without `endpoints`.

- [ ] **Step 5: Add temporary empty endpoint list at construction**

In `JavaParserService.parseSourceFile`, change the final `new ParsedSourceFile(...)` call to pass `List.of()` as the last argument. Keep this minimal; real parsing comes in Task 2.

- [ ] **Step 6: Run parser tests**

Run: `./mvnw test -Dtest=JavaParserServiceTest`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add docs/adr/0007-spring-endpoint-indexing.md \
  src/main/java/me/acharliekelly/hephaestus/indexing/ParsedEndpoint.java \
  src/main/java/me/acharliekelly/hephaestus/indexing/ParsedSourceFile.java \
  src/main/java/me/acharliekelly/hephaestus/indexing/JavaParserService.java
git commit -m "Add endpoint indexing ADR and parsed contract"
```

---

### Task 2: JavaParser Spring Endpoint Extraction

**Files:**
- Modify: `src/main/java/me/acharliekelly/hephaestus/indexing/JavaParserService.java`
- Modify: `src/test/java/me/acharliekelly/hephaestus/indexing/JavaParserServiceTest.java`

**Interfaces:**
- Consumes: `ParsedEndpoint` and `ParsedSourceFile.endpoints()`.
- Produces: endpoint facts from Spring annotations with normalized paths and HTTP methods.

- [ ] **Step 1: Add failing parser test**

Append this test to `JavaParserServiceTest`:

```java
@Test
void parsesSpringMvcEndpoints(@TempDir Path projectRoot) throws Exception {
    Path packageDir = Files.createDirectories(projectRoot.resolve("src/main/java/com/example/web"));
    Files.writeString(packageDir.resolve("FileController.java"), """
            package com.example.web;

            import org.springframework.web.bind.annotation.DeleteMapping;
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestBody;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            @RequestMapping("/api/files")
            public class FileController {
                @GetMapping
                public String list() {
                    return "ok";
                }

                @PostMapping({"/upload", "/imports"})
                public String upload(@RequestBody String body) {
                    return body;
                }

                @DeleteMapping("/{id}")
                public void delete(@PathVariable String id) {
                }
            }
            """);

    ParsedProject parsedProject = javaParserService.parse(projectRoot);

    assertThat(parsedProject.sourceFiles())
            .flatExtracting(ParsedSourceFile::endpoints)
            .extracting(
                    ParsedEndpoint::httpMethod,
                    ParsedEndpoint::path,
                    ParsedEndpoint::controllerQualifiedName,
                    ParsedEndpoint::handlerQualifiedName
            )
            .contains(
                    tuple("GET", "/api/files", "com.example.web.FileController",
                            "com.example.web.FileController#list()"),
                    tuple("POST", "/api/files/upload", "com.example.web.FileController",
                            "com.example.web.FileController#upload(String)"),
                    tuple("POST", "/api/files/imports", "com.example.web.FileController",
                            "com.example.web.FileController#upload(String)"),
                    tuple("DELETE", "/api/files/{id}", "com.example.web.FileController",
                            "com.example.web.FileController#delete(String)")
            );
}
```

- [ ] **Step 2: Verify red**

Run: `./mvnw test -Dtest=JavaParserServiceTest`

Expected: FAIL because `endpoints()` is empty.

- [ ] **Step 3: Implement endpoint parsing helpers**

In `JavaParserService`, add imports:

```java
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
```

Add `List<ParsedEndpoint> endpoints = new ArrayList<>();` beside symbols/dependencies.

Inside the `findAll(MethodDeclaration.class)` block, after method dependencies are added, call:

```java
endpoints.addAll(endpointDeclarations(typeDeclaration, methodDeclaration, ownerQualifiedName, methodQualifiedName));
```

Change the final `ParsedSourceFile` construction to pass `endpoints`.

Add these helpers:

```java
private List<ParsedEndpoint> endpointDeclarations(
        TypeDeclaration<?> typeDeclaration,
        MethodDeclaration methodDeclaration,
        String controllerQualifiedName,
        String handlerQualifiedName
) {
    if (!isController(typeDeclaration)) {
        return List.of();
    }
    List<String> classPaths = annotationPaths(typeDeclaration, "RequestMapping");
    if (classPaths.isEmpty()) {
        classPaths = List.of("");
    }
    return methodDeclaration.getAnnotations().stream()
            .flatMap(annotation -> endpointMethods(annotationName(annotation)).stream()
                    .flatMap(httpMethod -> {
                        List<String> methodPaths = annotationPaths(annotation);
                        if (methodPaths.isEmpty()) {
                            methodPaths = List.of("");
                        }
                        List<ParsedEndpoint> parsedEndpoints = new ArrayList<>();
                        for (String classPath : classPaths) {
                            for (String methodPath : methodPaths) {
                                parsedEndpoints.add(new ParsedEndpoint(
                                        httpMethod,
                                        combinePaths(classPath, methodPath),
                                        controllerQualifiedName,
                                        handlerQualifiedName,
                                        line(annotation)
                                ));
                            }
                        }
                        return parsedEndpoints.stream();
                    }))
            .toList();
}

private boolean isController(TypeDeclaration<?> typeDeclaration) {
    return typeDeclaration.getAnnotations().stream()
            .map(this::annotationName)
            .anyMatch(annotationName -> annotationName.equals("RestController") || annotationName.equals("Controller"));
}

private List<String> endpointMethods(String annotationName) {
    return switch (annotationName) {
        case "GetMapping" -> List.of("GET");
        case "PostMapping" -> List.of("POST");
        case "PutMapping" -> List.of("PUT");
        case "PatchMapping" -> List.of("PATCH");
        case "DeleteMapping" -> List.of("DELETE");
        case "RequestMapping" -> List.of("GET", "POST", "PUT", "PATCH", "DELETE");
        default -> List.of();
    };
}

private List<String> annotationPaths(NodeWithAnnotations<?> node, String annotationName) {
    return node.getAnnotations().stream()
            .filter(annotation -> annotationName(annotation).equals(annotationName))
            .flatMap(annotation -> annotationPaths(annotation).stream())
            .toList();
}

private List<String> annotationPaths(AnnotationExpr annotation) {
    if (annotation instanceof SingleMemberAnnotationExpr singleMemberAnnotationExpr) {
        return literalStrings(singleMemberAnnotationExpr.getMemberValue());
    }
    if (annotation instanceof NormalAnnotationExpr normalAnnotationExpr) {
        for (MemberValuePair pair : normalAnnotationExpr.getPairs()) {
            if (pair.getNameAsString().equals("value") || pair.getNameAsString().equals("path")) {
                return literalStrings(pair.getValue());
            }
        }
    }
    return List.of("");
}

private List<String> literalStrings(Expression expression) {
    if (expression.isStringLiteralExpr()) {
        return List.of(expression.asStringLiteralExpr().asString());
    }
    if (expression instanceof ArrayInitializerExpr arrayInitializerExpr) {
        return arrayInitializerExpr.getValues().stream()
                .filter(Expression::isStringLiteralExpr)
                .map(value -> value.asStringLiteralExpr().asString())
                .toList();
    }
    return List.of();
}

private String combinePaths(String classPath, String methodPath) {
    String combined = ("/" + stripSlashes(classPath) + "/" + stripSlashes(methodPath)).replaceAll("/+", "/");
    return combined.length() > 1 && combined.endsWith("/")
            ? combined.substring(0, combined.length() - 1)
            : combined;
}

private String stripSlashes(String path) {
    if (path == null || path.isBlank() || path.equals("/")) {
        return "";
    }
    String stripped = path;
    while (stripped.startsWith("/")) {
        stripped = stripped.substring(1);
    }
    while (stripped.endsWith("/")) {
        stripped = stripped.substring(0, stripped.length() - 1);
    }
    return stripped;
}
```

- [ ] **Step 4: Run parser tests**

Run: `./mvnw test -Dtest=JavaParserServiceTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/me/acharliekelly/hephaestus/indexing/JavaParserService.java \
  src/test/java/me/acharliekelly/hephaestus/indexing/JavaParserServiceTest.java
git commit -m "Parse Spring MVC endpoints"
```

---

### Task 3: Persist Endpoint Facts During Indexing

**Files:**
- Create: `src/main/java/me/acharliekelly/hephaestus/model/HttpMethod.java`
- Create: `src/main/java/me/acharliekelly/hephaestus/model/EndpointRecord.java`
- Create: `src/main/java/me/acharliekelly/hephaestus/model/persistence/EndpointRecordRepository.java`
- Modify: `src/main/java/me/acharliekelly/hephaestus/indexing/IndexingService.java`
- Modify: `src/test/java/me/acharliekelly/hephaestus/indexing/IndexingServiceTest.java`

**Interfaces:**
- Produces: `EndpointRecord` entity with repository, source file, controller symbol, handler symbol, HTTP method, path, line number.
- Produces: `EndpointRecordRepository.findByRepositoryId(Long repositoryId)`.

- [ ] **Step 1: Add failing indexing test**

Append to `IndexingServiceTest` and autowire `EndpointRecordRepository endpoints;`:

```java
@Autowired
private EndpointRecordRepository endpoints;

@Test
void persistsEndpointFactsAndReindexingDoesNotDuplicateThem(@TempDir Path projectRoot) throws Exception {
    Path packageDir = Files.createDirectories(projectRoot.resolve("src/main/java/com/example/web"));
    Files.writeString(packageDir.resolve("FileController.java"), """
            package com.example.web;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            @RequestMapping("/api/files")
            public class FileController {
                @GetMapping("/{id}")
                public String getFile(@PathVariable String id) {
                    return id;
                }
            }
            """);
    RepositoryRecord repository = repoService.importLocal(projectRoot.toString());

    indexingService.indexRepository(repository.getId());
    indexingService.indexRepository(repository.getId());

    assertThat(endpoints.findByRepositoryId(repository.getId()))
            .hasSize(1)
            .extracting(endpoint -> endpoint.getHttpMethod().name(), EndpointRecord::getPath)
            .containsExactly(tuple("GET", "/api/files/{id}"));
}
```

- [ ] **Step 2: Verify red**

Run: `./mvnw test -Dtest=IndexingServiceTest`

Expected: compile fails because endpoint persistence classes do not exist.

- [ ] **Step 3: Add HTTP method enum**

Create `src/main/java/me/acharliekelly/hephaestus/model/HttpMethod.java`:

```java
package me.acharliekelly.hephaestus.model;

public enum HttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE
}
```

- [ ] **Step 4: Add endpoint entity**

Create `src/main/java/me/acharliekelly/hephaestus/model/EndpointRecord.java`:

```java
package me.acharliekelly.hephaestus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "endpoints",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"repository_id", "httpMethod", "path", "handler_symbol_id"}
        )
)
public class EndpointRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private RepositoryRecord repository;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_file_id", nullable = false)
    private SourceFileRecord sourceFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "controller_symbol_id")
    private CodeSymbolRecord controllerSymbol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handler_symbol_id")
    private CodeSymbolRecord handlerSymbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HttpMethod httpMethod;

    @Column(nullable = false, length = 2048)
    private String path;

    private Integer lineNumber;

    protected EndpointRecord() {
    }

    public EndpointRecord(
            RepositoryRecord repository,
            SourceFileRecord sourceFile,
            CodeSymbolRecord controllerSymbol,
            CodeSymbolRecord handlerSymbol,
            HttpMethod httpMethod,
            String path,
            Integer lineNumber
    ) {
        this.repository = repository;
        this.sourceFile = sourceFile;
        this.controllerSymbol = controllerSymbol;
        this.handlerSymbol = handlerSymbol;
        this.httpMethod = httpMethod;
        this.path = path;
        this.lineNumber = lineNumber;
    }

    public Long getId() { return id; }
    public RepositoryRecord getRepository() { return repository; }
    public SourceFileRecord getSourceFile() { return sourceFile; }
    public CodeSymbolRecord getControllerSymbol() { return controllerSymbol; }
    public CodeSymbolRecord getHandlerSymbol() { return handlerSymbol; }
    public HttpMethod getHttpMethod() { return httpMethod; }
    public String getPath() { return path; }
    public Integer getLineNumber() { return lineNumber; }
}
```

- [ ] **Step 5: Add endpoint repository**

Create `src/main/java/me/acharliekelly/hephaestus/model/persistence/EndpointRecordRepository.java`:

```java
package me.acharliekelly.hephaestus.model.persistence;

import java.util.List;
import me.acharliekelly.hephaestus.model.EndpointRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EndpointRecordRepository extends JpaRepository<EndpointRecord, Long> {
    @EntityGraph(attributePaths = {"sourceFile", "controllerSymbol", "handlerSymbol"})
    List<EndpointRecord> findByRepositoryId(Long repositoryId);

    void deleteByRepositoryId(Long repositoryId);
}
```

- [ ] **Step 6: Wire endpoint persistence into indexing**

Modify `IndexingService` constructor to accept `EndpointRecordRepository endpoints`.

Delete endpoint rows before dependencies:

```java
endpoints.deleteByRepositoryId(repositoryId);
endpoints.flush();
```

After dependency persistence, add:

```java
for (ParsedSourceFile parsedSourceFile : parsedProject.sourceFiles()) {
    SourceFileRecord sourceFile = savedSourceFiles.get(parsedSourceFile);
    for (ParsedEndpoint parsedEndpoint : parsedSourceFile.endpoints()) {
        endpoints.save(new EndpointRecord(
                repository,
                sourceFile,
                savedSymbols.get(parsedEndpoint.controllerQualifiedName()),
                savedSymbols.get(parsedEndpoint.handlerQualifiedName()),
                HttpMethod.valueOf(parsedEndpoint.httpMethod()),
                parsedEndpoint.path(),
                parsedEndpoint.lineNumber()
        ));
    }
}
```

- [ ] **Step 7: Run indexing tests**

Run: `./mvnw test -Dtest=IndexingServiceTest`

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/me/acharliekelly/hephaestus/model/HttpMethod.java \
  src/main/java/me/acharliekelly/hephaestus/model/EndpointRecord.java \
  src/main/java/me/acharliekelly/hephaestus/model/persistence/EndpointRecordRepository.java \
  src/main/java/me/acharliekelly/hephaestus/indexing/IndexingService.java \
  src/test/java/me/acharliekelly/hephaestus/indexing/IndexingServiceTest.java
git commit -m "Persist indexed Spring endpoints"
```

---

### Task 4: Endpoint Query Service And REST API

**Files:**
- Create: `src/main/java/me/acharliekelly/hephaestus/graph/EndpointQueryService.java`
- Create: `src/main/java/me/acharliekelly/hephaestus/web/EndpointResponse.java`
- Create: `src/main/java/me/acharliekelly/hephaestus/web/EndpointController.java`
- Create: `src/test/java/me/acharliekelly/hephaestus/graph/EndpointQueryServiceTest.java`
- Create: `src/test/java/me/acharliekelly/hephaestus/web/EndpointApiTest.java`

**Interfaces:**
- Produces: `EndpointQueryService.findEndpoints(Long repositoryId, String path)`
- Produces: `GET /api/repositories/{repositoryId}/endpoints`
- Produces: `GET /api/repositories/{repositoryId}/endpoints?path=/api/files/{id}`

- [ ] **Step 1: Add failing service test**

Create `src/test/java/me/acharliekelly/hephaestus/graph/EndpointQueryServiceTest.java`:

```java
package me.acharliekelly.hephaestus.graph;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import me.acharliekelly.hephaestus.indexing.IndexingService;
import me.acharliekelly.hephaestus.model.RepositoryRecord;
import me.acharliekelly.hephaestus.repo.RepoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EndpointQueryServiceTest {
    @Autowired
    private RepoService repoService;

    @Autowired
    private IndexingService indexingService;

    @Autowired
    private EndpointQueryService endpointQueryService;

    @Test
    void findsEndpointsWithOptionalPathFilter(@TempDir Path projectRoot) throws Exception {
        writeController(projectRoot);
        RepositoryRecord repository = repoService.importLocal(projectRoot.toString());
        indexingService.indexRepository(repository.getId());

        assertThat(endpointQueryService.findEndpoints(repository.getId(), ""))
                .extracting(endpoint -> endpoint.getHttpMethod().name(), endpoint -> endpoint.getPath())
                .containsExactlyInAnyOrder(
                        org.assertj.core.api.Assertions.tuple("GET", "/api/files"),
                        org.assertj.core.api.Assertions.tuple("POST", "/api/files")
                );

        assertThat(endpointQueryService.findEndpoints(repository.getId(), "/api/files"))
                .hasSize(2);
    }

    private void writeController(Path projectRoot) throws Exception {
        Path packageDir = Files.createDirectories(projectRoot.resolve("src/main/java/com/example/web"));
        Files.writeString(packageDir.resolve("FileController.java"), """
                package com.example.web;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/api/files")
                public class FileController {
                    @GetMapping
                    public String list() { return "ok"; }

                    @PostMapping
                    public String create() { return "ok"; }
                }
                """);
    }
}
```

- [ ] **Step 2: Verify red**

Run: `./mvnw test -Dtest=EndpointQueryServiceTest`

Expected: compile fails because `EndpointQueryService` does not exist.

- [ ] **Step 3: Implement service**

Create `src/main/java/me/acharliekelly/hephaestus/graph/EndpointQueryService.java`:

```java
package me.acharliekelly.hephaestus.graph;

import java.util.Comparator;
import java.util.List;
import me.acharliekelly.hephaestus.model.EndpointRecord;
import me.acharliekelly.hephaestus.model.persistence.EndpointRecordRepository;
import me.acharliekelly.hephaestus.repo.RepoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EndpointQueryService {
    private final RepoService repoService;
    private final EndpointRecordRepository endpoints;

    public EndpointQueryService(RepoService repoService, EndpointRecordRepository endpoints) {
        this.repoService = repoService;
        this.endpoints = endpoints;
    }

    @Transactional(readOnly = true)
    public List<EndpointRecord> findEndpoints(Long repositoryId, String path) {
        repoService.requireRepository(repositoryId);
        String pathFilter = path == null ? "" : path;
        return endpoints.findByRepositoryId(repositoryId).stream()
                .filter(endpoint -> pathFilter.isBlank() || endpoint.getPath().contains(pathFilter))
                .sorted(Comparator.comparing(EndpointRecord::getPath)
                        .thenComparing(endpoint -> endpoint.getHttpMethod().name()))
                .toList();
    }
}
```

- [ ] **Step 4: Run service test**

Run: `./mvnw test -Dtest=EndpointQueryServiceTest`

Expected: PASS.

- [ ] **Step 5: Add API response and controller**

Create `src/main/java/me/acharliekelly/hephaestus/web/EndpointResponse.java`:

```java
package me.acharliekelly.hephaestus.web;

import me.acharliekelly.hephaestus.model.EndpointRecord;

public record EndpointResponse(
        Long id,
        String httpMethod,
        String path,
        Long controllerSymbolId,
        Long handlerSymbolId,
        String sourcePath,
        Integer lineNumber
) {
    public static EndpointResponse from(EndpointRecord endpoint) {
        return new EndpointResponse(
                endpoint.getId(),
                endpoint.getHttpMethod().name(),
                endpoint.getPath(),
                endpoint.getControllerSymbol() == null ? null : endpoint.getControllerSymbol().getId(),
                endpoint.getHandlerSymbol() == null ? null : endpoint.getHandlerSymbol().getId(),
                endpoint.getSourceFile().getRelativePath(),
                endpoint.getLineNumber()
        );
    }
}
```

Create `src/main/java/me/acharliekelly/hephaestus/web/EndpointController.java`:

```java
package me.acharliekelly.hephaestus.web;

import java.util.List;
import me.acharliekelly.hephaestus.graph.EndpointQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repositories/{repositoryId}/endpoints")
public class EndpointController {
    private final EndpointQueryService endpointQueryService;

    public EndpointController(EndpointQueryService endpointQueryService) {
        this.endpointQueryService = endpointQueryService;
    }

    @GetMapping
    public List<EndpointResponse> endpoints(
            @PathVariable Long repositoryId,
            @RequestParam(defaultValue = "") String path
    ) {
        return endpointQueryService.findEndpoints(repositoryId, path).stream()
                .map(EndpointResponse::from)
                .toList();
    }
}
```

- [ ] **Step 6: Add API test**

Create `src/test/java/me/acharliekelly/hephaestus/web/EndpointApiTest.java`:

```java
package me.acharliekelly.hephaestus.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EndpointApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listsIndexedEndpoints(@TempDir Path projectRoot) throws Exception {
        writeController(projectRoot);

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

        String endpointsJson = mockMvc.perform(get("/api/repositories/{repositoryId}/endpoints", repositoryId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<EndpointResponse> endpoints = objectMapper.readValue(endpointsJson, new TypeReference<>() {
        });

        assertThat(endpoints)
                .extracting(EndpointResponse::httpMethod, EndpointResponse::path)
                .contains(org.assertj.core.api.Assertions.tuple("GET", "/api/files"));
    }

    private void writeController(Path projectRoot) throws Exception {
        Path packageDir = Files.createDirectories(projectRoot.resolve("src/main/java/com/example/web"));
        Files.writeString(packageDir.resolve("FileController.java"), """
                package com.example.web;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/api/files")
                public class FileController {
                    @GetMapping
                    public String list() {
                        return "ok";
                    }
                }
                """);
    }
}
```

- [ ] **Step 7: Run API test**

Run: `./mvnw test -Dtest=EndpointApiTest`

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/me/acharliekelly/hephaestus/graph/EndpointQueryService.java \
  src/main/java/me/acharliekelly/hephaestus/web/EndpointResponse.java \
  src/main/java/me/acharliekelly/hephaestus/web/EndpointController.java \
  src/test/java/me/acharliekelly/hephaestus/graph/EndpointQueryServiceTest.java \
  src/test/java/me/acharliekelly/hephaestus/web/EndpointApiTest.java
git commit -m "Expose indexed endpoint queries"
```

---

### Task 5: README, Verification, And Manual Test Commands

**Files:**
- Modify: `README.md`

**Interfaces:**
- Documents: `GET /api/repositories/{repositoryId}/endpoints`
- Documents: `GET /api/repositories/{repositoryId}/endpoints?path=/api/files`

- [ ] **Step 1: Update README API section**

Add after "Architecture Summary":

````markdown
### Query Spring Endpoints

```bash
curl http://localhost:8080/api/repositories/{repositoryId}/endpoints
curl 'http://localhost:8080/api/repositories/{repositoryId}/endpoints?path=/api/files'
```
````

- [ ] **Step 2: Run full verification**

Run: `./mvnw test`

Expected: BUILD SUCCESS, all tests pass.

- [ ] **Step 3: Manual smoke test**

Run:

```bash
./mvnw spring-boot:run
```

In another terminal, run:

```bash
curl -X POST http://localhost:8080/api/repositories/import-local \
  -H 'Content-Type: application/json' \
  -d '{"path":"/absolute/path/to/spring/project"}'
curl -X POST http://localhost:8080/api/repositories/1/index
curl http://localhost:8080/api/repositories/1/endpoints
```

Expected: endpoint JSON includes `httpMethod`, `path`, `sourcePath`, and `lineNumber`.

- [ ] **Step 4: Commit**

```bash
git add README.md
git commit -m "Document endpoint query API"
```

---

## Self-Review

Spec coverage:
- ADR included because the slice adds persisted facts and public APIs.
- Parser support includes all first-slice Spring MVC annotations named in the goal.
- Persistence includes source evidence and symbol links.
- API exposes repository endpoint lookup and path filtering.
- Tests cover parser, indexing/re-indexing, query service, and API.

Placeholder scan:
- No `TBD`, `TODO`, or deferred implementation placeholders remain.
- Dynamic/computed path expressions are explicitly out of scope in ADR text.

Type consistency:
- `ParsedEndpoint` fields are consumed by `IndexingService`.
- `EndpointRecord` fields are consumed by `EndpointQueryService` and `EndpointResponse`.
- API paths match README commands.
