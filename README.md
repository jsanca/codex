# Codex

Codex is a headless, multi-tenant, multi-language CMS conceived as a disciplined modular monolith.

It is not intended to be a traditional page-centric CMS. The center of Codex is the domain: structured content, revision-aware lifecycle, workflow-driven operations, and extensibility points that allow the system to evolve without bloating the core.

This repository is being built in phases. The current phase establishes the modular skeleton, naming, and architectural boundaries before deeper implementation work begins.

## Current Direction

Codex is being designed around a small but expressive core.

At a high level:

- **Codex** defines what content is.
- **Chronicon** remembers how content changes over time.
- **Archivum** concerns itself with storage.
- **Index** supports search and discoverability.
- **Scriptorium** is the space for scripting and customization.
- **Illuminarium** is the future domain of enrichment and semantic enhancement.
- **Porta** is the external boundary of the system.
- **Iter** governs workflow and orchestration.
- **Custos** is the identity and access-control boundary.
- **Imaginarium** is the AI integration layer.
- **Olórin** is the agent layer.

This separation is intentional.

Imaginarium is not the domain agent itself. It exists to provide the infrastructure required for AI integration, orchestration, and interoperability. Olórin is the component responsible for reasoning over the Codex domain, planning meaningful actions, and operating through explicit capabilities.

## Phase 1

Phase 1 is intentionally limited.

Its purpose is to establish:

- the Maven multi-module structure,
- Java module descriptors (Jigsaw),
- package roots,
- public vs internal module boundaries,
- and naming consistency aligned with the Codex lore.

Phase 1 does **not** yet implement the Codex MVP behavior.

It also does **not** settle deeper technical decisions such as:

- persistence strategy,
- JPA vs JDBC vs other data access approaches,
- JSONB mappings,
- REST controllers,
- OpenSearch integration,
- S3 integration,
- workflow engine behavior,
- and full business use cases.

## Module Map

The current conceptual and technical modules are:

- `codex-fundamentum`
- `codex-codex`
- `codex-chronicon`
- `codex-archivum`
- `codex-index`
- `codex-scriptorium`
- `codex-illuminarium`
- `codex-porta`
- `codex-iter`
- `codex-custos`
- `codex-imaginarium`
- `codex-olorin`

## Package Convention

The global base package is `codex`.

Each module follows this convention:

- `codex.<module>`
- `codex.<module>.api`
- `codex.<module>.internal`

The intent is to make public contracts explicit and keep implementation details clearly separated.

## Architecture

Codex is currently organized into the following Phase 1 Jigsaw modules:

- **Fundamentum** (`codex-fundamentum`) — Foundational shared abstractions and cross-cutting base types.
- **Codex** (`codex-codex`) — The central domain/kernel of the system.
- **Chronicon** (`codex-chronicon`) — History, revision memory, and publication narrative.
- **Archivum** (`codex-archivum`) — Storage abstraction layer.
- **Index** (`codex-index`) — Search and discoverability.
- **Scriptorium** (`codex-scriptorium`) — Scripting and dynamic customization.
- **Illuminarium** (`codex-illuminarium`) — Enrichment and semantic enhancement.
- **Porta** (`codex-porta`) — External exposure layer, including APIs and integrations.
- **Iter** (`codex-iter`) — Workflow and orchestration.
- **Custos** (`codex-custos`) — Identity, roles, permissions, and access control.
- **Imaginarium** (`codex-imaginarium`) — AI integration infrastructure.
- **Olórin** (`codex-olorin`) — Agent-oriented reasoning and domain-aware interaction.

## Build

This project uses **Maven** and **Java Modules (Jigsaw)**.

To verify the current multi-module skeleton from the repository root:

```bash
mvn clean verify
```

## Documentation

Architectural notes, lore, specs, and future ADRs live in `codex-docs`.

This root README is intended to remain an entry point: concise, repository-oriented, and focused on helping readers understand what Codex is, how the repository is organized, and what phase of the project is currently in progress.

Module-specific intent and architectural details should live in the corresponding module documentation.

## Status

Codex is in an early architectural phase.

The structure now exists so that implementation can proceed incrementally and deliberately, with decisions documented as they become stable.