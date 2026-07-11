# JavaParser Java 21 Diagnostics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Hephaestus parse Java 21 source constructs deterministically and return controlled indexing errors for parse failures instead of raw server errors.

**Architecture:** Keep Hephaestus itself on the Java 21 Spring Boot baseline from ADR 0002. Configure the existing JavaParser-based indexing flow from ADR 0004 to parse Java 21 source syntax, then convert JavaParser parse failures into the existing `IndexingException` path so the REST API returns `422 Unprocessable Entity`.

**Tech Stack:** Java 21 target, Spring Boot 3.3.5, Maven wrapper, JavaParser 3.26.2, JUnit 5, AssertJ, MockMvc.

## Global Constraints

- Follow ADR rules in `AGENTS.md`; this plan preserves ADR 0002 and ADR 0004, so no new ADR is required.
- Do not change `pom.xml` `<java.version>21</java.version>`.
- Do not change the Maven wrapper or Spring Boot version.
- Keep package root `me.acharliekelly.hephaestus`.
- Keep JavaParser as the deterministic static analysis engine.
- Do not create or modify Git worktrees; use a normal local branch in the current checkout.
- Keep parse failures deterministic and non-AI.
- Run `./mvnw test` before completion.

---

## File Structure

Modify:
- `src/main/java/me/acharliekelly/hephaestus/indexing/JavaParserService.java`: configure JavaParser for Java 21 and wrap parse failures.
- `src/test/java/me/acharliekelly/hephaestus/indexing/JavaParserServiceTest.java`: add Java 21 source parsing regression coverage.
- `src/test/java/me/acharliekelly/hephaestus/web/ApiIntegrationTest.java`: add API regression coverage for parse failures returning `422`.
- `README.md`: document that Java 21 is the project target and that newer local JDKs can run the Maven wrapper.

No new persistence table, response DTO, or public endpoint is introduced in this slice.

---

### Task 1: Configure JavaParser For Java 21 Source

**Files:**
- Modify: `src/test/java/me/acharliekelly/hephaestus/indexing/JavaParserServiceTest.java`
- Modify: `src/main/java/me/acharliekelly/hephaestus/indexing/JavaParserService.java`

**Interfaces:**
- Consumes: `JavaParserService.parse(Path projectRoot)`
- Produces: `JavaParserService` configured with `ParserConfiguration.LanguageLevel.JAVA_21`

- [ ] **Step 1: Create the local branch**

Run:

```bash
git switch -c bugfix/javaparser-java21-diagnostics
```

Expected: branch changes to `bugfix/javaparser-java21-diagnostics`.

- [ ] **Step 2: Add a failing Java 21 parser regression test**

Append this test to `src/test/java/me/acharliekelly/hephaestus/indexing/JavaParserServiceTest.java` before the `tuple` helper:

```java
    @Test
    void parsesJava21RecordTextBlockAndSwitchExpression(@TempDir Path projectRoot) throws Exception {
        Path packageDir = Files.createDirectories(projectRoot.resolve("src/main/java/com/example/payments"));
        Files.writeString(packageDir.resolve("PaymentCommand.java"), """
                package com.example.payments;

                public record PaymentCommand(String accountId, int cents) {
                    public String description() {
                        return \"""
                                payment:%s:%d
                                \""".formatted(accountId, cents);
                    }
                }
                """);
        Files.writeString(packageDir.resolve("PaymentService.java"), """
                package com.example.payments;

                public class PaymentService {
                    public String describe(PaymentCommand command) {
                        return switch (command.cents()) {
                            case 0 -> "free";
                            default -> command.description();
                        };
                    }
                }
                """);

        ParsedProject parsedProject = javaParserService.parse(projectRoot);

        assertThat(parsedProject.sourceFiles()).hasSize(2);
        assertThat(parsedProject.sourceFiles())
                .flatExtracting(ParsedSourceFile::symbols)
                .extracting(ParsedSymbol::qualifiedName, ParsedSymbol::kind)
                .contains(
                        tuple("com.example.payments.PaymentCommand", SymbolKind.CLASS),
                        tuple("com.example.payments.PaymentCommand#description()", SymbolKind.METHOD),
                        tuple("com.example.payments.PaymentService", SymbolKind.CLASS),
                        tuple("com.example.payments.PaymentService#describe(PaymentCommand)", SymbolKind.METHOD)
                );
        assertThat(parsedProject.sourceFiles())
                .flatExtracting(ParsedSourceFile::dependencies)
                .filteredOn(dependency -> dependency.kind() == DependencyKind.METHOD_CALL)
                .extracting(ParsedDependency::fromSymbolQualifiedName, ParsedDependency::targetName)
                .contains(
                        tuple("com.example.payments.PaymentCommand#description()", "formatted"),
                        tuple("com.example.payments.PaymentService#describe(PaymentCommand)", "description")
                );
    }
```

- [ ] **Step 3: Verify red**

Run:

```bash
./mvnw test -Dtest=JavaParserServiceTest
```

Expected: FAIL because JavaParser rejects the record declaration with a message containing `Record Declarations are not supported`.

- [ ] **Step 4: Configure JavaParser language level**

In `src/main/java/me/acharliekelly/hephaestus/indexing/JavaParserService.java`, add this import:

```java
import com.github.javaparser.ParserConfiguration;
```

Add this constructor immediately inside the `JavaParserService` class:

```java
    public JavaParserService() {
        StaticJavaParser.getParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }
```

The top of the class should include the constructor before `parse`:

```java
@Service
public class JavaParserService {
    public JavaParserService() {
        StaticJavaParser.getParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    public ParsedProject parse(Path projectRoot) {
        try (var paths = Files.walk(projectRoot)) {
```

- [ ] **Step 5: Verify green**

Run:

```bash
./mvnw test -Dtest=JavaParserServiceTest
```

Expected: PASS, including the new Java 21 regression test.

- [ ] **Step 6: Commit**

Run:

```bash
git add src/main/java/me/acharliekelly/hephaestus/indexing/JavaParserService.java \
  src/test/java/me/acharliekelly/hephaestus/indexing/JavaParserServiceTest.java
git commit -m "Configure JavaParser for Java 21 source"
```

---

### Task 2: Return Controlled API Errors For Parse Failures

**Files:**
- Modify: `src/test/java/me/acharliekelly/hephaestus/web/ApiIntegrationTest.java`
- Modify: `src/main/java/me/acharliekelly/hephaestus/indexing/JavaParserService.java`

**Interfaces:**
- Consumes: `ApiExceptionHandler.indexingFailed(IndexingException exception)`
- Produces: `IndexingException("Failed to parse <path>: <parser message>", cause)` for JavaParser parse failures

- [ ] **Step 1: Add the failing API regression test**

Append this test to `src/test/java/me/acharliekelly/hephaestus/web/ApiIntegrationTest.java` before the `writeFixture` helper:

```java
    @Test
    void parseFailuresReturnUnprocessableEntity(@TempDir Path projectRoot) throws Exception {
        Path packageDir = Files.createDirectories(projectRoot.resolve("src/main/java/com/example"));
        Files.writeString(packageDir.resolve("BrokenService.java"), """
                package com.example;

                public class BrokenService {
                    public String broken() {
                        return "missing close brace";
                """);

        String importJson = mockMvc.perform(post("/api/repositories/import-local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ImportLocalRepositoryRequest(projectRoot.toString()))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long repositoryId = objectMapper.readValue(importJson, RepositoryResponse.class).id();

        String errorJson = mockMvc.perform(post("/api/repositories/{repositoryId}/index", repositoryId))
                .andExpect(status().isUnprocessableEntity())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readValue(errorJson, ErrorResponse.class).message())
                .contains("Failed to parse")
                .contains("BrokenService.java");
    }
```

- [ ] **Step 2: Verify red**

Run:

```bash
./mvnw test -Dtest=ApiIntegrationTest
```

Expected: FAIL because parse failures currently escape as a raw server error instead of the existing `IndexingException` API path.

- [ ] **Step 3: Wrap JavaParser parse failures**

In `src/main/java/me/acharliekelly/hephaestus/indexing/JavaParserService.java`, add this import:

```java
import com.github.javaparser.ParseProblemException;
```

Change the `parseSourceFile` catch block from:

```java
        } catch (IOException ex) {
            throw new IndexingException("Failed to parse " + sourceFile, ex);
        }
```

to:

```java
        } catch (IOException | ParseProblemException ex) {
            throw new IndexingException("Failed to parse " + sourceFile + ": " + ex.getMessage(), ex);
        }
```

- [ ] **Step 4: Verify API test passes**

Run:

```bash
./mvnw test -Dtest=ApiIntegrationTest
```

Expected: PASS, including `parseFailuresReturnUnprocessableEntity`.

- [ ] **Step 5: Run focused parser and API tests together**

Run:

```bash
./mvnw test -Dtest=JavaParserServiceTest,ApiIntegrationTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```bash
git add src/main/java/me/acharliekelly/hephaestus/indexing/JavaParserService.java \
  src/test/java/me/acharliekelly/hephaestus/web/ApiIntegrationTest.java
git commit -m "Return parse failures as indexing errors"
```

---

### Task 3: Document Java Baseline And Verify The Slice

**Files:**
- Modify: `README.md`

**Interfaces:**
- Documents: Java 21 remains the project source compatibility target.
- Documents: Maven may run under a newer locally installed JDK while compiling the project as Java 21.

- [ ] **Step 1: Update README requirements text**

In `README.md`, replace the current Requirements section:

```markdown
## Requirements

- Java 21
- Bash-compatible shell for `./mvnw`

Maven does not need to be installed globally; this repository includes a Maven wrapper.
```

with:

```markdown
## Requirements

- Java 21 or newer JDK for running the Maven wrapper.
- Bash-compatible shell for `./mvnw`.

The project source compatibility target remains Java 21. A newer local JDK can run
the build, but project code should not require Java features beyond the Java 21
baseline unless a later ADR supersedes ADR 0002.

Maven does not need to be installed globally; this repository includes a Maven wrapper.
```

- [ ] **Step 2: Run full verification**

Run:

```bash
./mvnw test
```

Expected: BUILD SUCCESS with all tests passing.

- [ ] **Step 3: Manual smoke test on a Java 21 fixture**

Create this temporary fixture outside the repo:

```bash
mkdir -p /tmp/hephaestus-java21-smoke/src/main/java/com/example
cat > /tmp/hephaestus-java21-smoke/src/main/java/com/example/PaymentCommand.java <<'JAVA'
package com.example;

public record PaymentCommand(String accountId, int cents) {
    public String description() {
        return """
                payment:%s:%d
                """.formatted(accountId, cents);
    }
}
JAVA
cat > /tmp/hephaestus-java21-smoke/src/main/java/com/example/PaymentService.java <<'JAVA'
package com.example;

public class PaymentService {
    public String describe(PaymentCommand command) {
        return switch (command.cents()) {
            case 0 -> "free";
            default -> command.description();
        };
    }
}
JAVA
```

Start the app on an alternate port if 8080 is occupied:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=18080
```

In another terminal, run:

```bash
curl -s -X POST http://localhost:18080/api/repositories/import-local \
  -H 'Content-Type: application/json' \
  -d '{"path":"/tmp/hephaestus-java21-smoke"}'
curl -s -X POST http://localhost:18080/api/repositories/1/index
curl -s 'http://localhost:18080/api/repositories/1/symbols?name=PaymentCommand'
```

Expected:
- Import response contains `"id":1`.
- Index response contains `"sourceFileCount":2`.
- Symbol response contains `"qualifiedName":"com.example.PaymentCommand"`.

Stop the app with `Ctrl-C` after the smoke test.

- [ ] **Step 4: Commit**

Run:

```bash
git add README.md
git commit -m "Document Java 21 compatibility target"
```

---

## Self-Review

Spec coverage:
- Java 21 source parsing is covered by `parsesJava21RecordTextBlockAndSwitchExpression`.
- Parse failures are covered through the REST API by `parseFailuresReturnUnprocessableEntity`.
- ADR 0002 is preserved because the Maven Java target remains `21`.
- ADR 0004 is preserved because JavaParser remains the static analysis engine.

Placeholder scan:
- No deferred implementation placeholders remain.
- Every task has exact file paths, code snippets, commands, and expected outcomes.

Type consistency:
- `JavaParserService.parse(Path)` remains unchanged for callers.
- `IndexingException` continues to be handled by `ApiExceptionHandler.indexingFailed`.
- README text matches the unchanged `pom.xml` Java target.
