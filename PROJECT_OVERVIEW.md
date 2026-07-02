# PROJECT OVERVIEW

## Working Title

**Hephaestus**

---

# Vision

Hephaestus is an AI-assisted architecture explorer for Java applications.

Instead of asking a large language model to "guess" how a project works, Hephaestus performs deterministic static analysis of a Java codebase, builds an internal representation of its architecture, and then uses an LLM to explain that architecture, answer questions, generate diagrams, and assist developers in understanding unfamiliar systems.

The guiding philosophy is:

> **Facts come from code. Explanations come from AI.**

The AI should never be responsible for discovering relationships that can be determined through static analysis.

---

# Goals

Hephaestus should be capable of answering questions such as:

- Explain the overall architecture of this project.
- Describe the purpose of this package.
- What happens if I modify this class?
- Which classes depend on this service?
- Which REST endpoints exist?
- Where is this interface implemented?
- Are there circular dependencies?
- Which tests exercise this component?
- Generate an architecture diagram.
- Explain this project to a new developer.

Eventually, the project may also support:

- architectural reviews
- code smell detection
- automated documentation
- pull request analysis
- refactoring recommendations

These are intentionally considered future enhancements.

---

# Core Design Principle

Hephaestus separates **analysis** from **reasoning**.

```
             Java Source
                  │
                  ▼
         Static Analysis Engine
                  │
                  ▼
          Architecture Graph
                  │
         ┌────────┴────────┐
         ▼                 ▼
Semantic Index       Deterministic Queries
         │                 │
         └────────┬────────┘
                  ▼
             Spring AI
                  │
                  ▼
           Human-readable answers
```

The LLM is never the source of truth.

---

# High-Level Architecture

## 1. Repository Workspace

Responsible for:

- cloning repositories
- managing local workspaces
- checking out branches
- cleaning temporary directories

Owns all Git interactions.

---

## 2. Code Indexer

Responsible for parsing Java source.

Initial implementation will use JavaParser.

Extracted information includes:

- packages
- classes
- interfaces
- enums
- methods
- annotations
- imports
- inheritance
- implemented interfaces
- method calls
- field references

No AI is used in this layer.

---

## 3. Architecture Graph

Stores deterministic relationships discovered during parsing.

Examples:

- Controller → Service
- Service → Repository
- Class → Dependency
- Test → Production Class
- Package → Package

This layer represents the project's factual structure.

Initial implementation may use relational storage.

Graph databases (Neo4j, etc.) may be evaluated later.

---

## 4. Semantic Index

Stores embeddings of meaningful code and documentation.

Possible indexed content:

- source files
- class summaries
- method summaries
- package descriptions
- README files
- build files
- configuration

This enables Retrieval-Augmented Generation (RAG).

---

## 5. Conversation Layer

Responsible for converting user questions into execution plans.

Example:

Question:

> What breaks if I change PaymentService?

Execution plan:

1. Query dependency graph.
2. Retrieve relevant source.
3. Retrieve semantic context.
4. Invoke LLM.
5. Generate response with citations.

This layer orchestrates reasoning but does not discover architectural facts.

---

# Suggested Package Structure

```
me.acharliekelly.hephaestus

config/

repo/
    RepoController
    RepoService
    GitService
    WorkspaceService

indexing/
    IndexingService
    JavaParserService
    ChunkingService

graph/
    ArchitectureGraphService
    DependencyQueryService

search/
    SemanticSearchService
    EmbeddingService

ai/
    ArchitectChatService
    PromptTemplates
    ToolDefinitions

model/
    Repository
    SourceFile
    CodeSymbol
    Dependency
    CodeChunk

web/
    ChatController
    ImportController
```

The application should begin as a **modular monolith**.

Premature decomposition into microservices is intentionally avoided.

---

# Data Model

Initial entities:

- Repository
- SourceFile
- CodeSymbol
- Dependency
- CodeChunk
- Conversation
- Message

Hephaestus intentionally stores two kinds of knowledge:

## Symbolic Knowledge

Deterministic facts.

Examples:

- class hierarchy
- dependency graph
- package relationships

## Semantic Knowledge

Embeddings used for natural-language retrieval.

Examples:

- documentation
- implementation summaries
- code context

Both are required for high-quality responses.

---

# AI Philosophy

Spring AI should primarily be used for:

- summarization
- explanation
- question answering
- diagram generation
- documentation

It should **not** determine:

- dependency relationships
- inheritance
- method call graphs
- architectural structure

Those are discovered through deterministic analysis.

---

# AI Tooling

The LLM should eventually have access to application tools rather than raw source.

Potential tools include:

- findClass(name)
- findDependents(symbol)
- findDependencies(symbol)
- searchCode(query)
- getFile(path)
- generateMermaidDiagram(scope)

This keeps prompts smaller while improving factual accuracy.

---

# Initial Milestone (MVP)

Hephaestus version 0.1 should support:

- Import a local Java project
- Parse Java source
- Build dependency graph
- Query dependency relationships
- Generate architecture summaries
- Answer basic architecture questions
- Produce Mermaid dependency diagrams

The MVP should not modify source code.

---

# Future Ideas

Potential future capabilities include:

## Documentation

- ADR generation
- README generation
- onboarding guides

## Architecture Review

- circular dependency detection
- package cohesion analysis
- layering violations
- dependency inversion violations

## Testing

- test coverage visualization
- missing unit test detection
- integration test recommendations

## Git Integration

- explain pull requests
- summarize architectural impact
- compare branches
- identify risky changes

## Autonomous Development

Long-term exploration may include:

- automated refactoring
- code review assistance
- iterative test generation
- architecture-aware coding agents

These features should build upon the deterministic architecture model rather than replacing it.

---

# Development Philosophy

1. Make deterministic analysis trustworthy.
2. Make AI helpful, not authoritative.
3. Prefer explicit architecture over hidden magic.
4. Keep modules independently testable.
5. Build features that would genuinely help a senior engineer understand an unfamiliar codebase.

If Hephaestus cannot explain *why* it reached a conclusion, it should expose the underlying evidence rather than asking the user to trust the model.