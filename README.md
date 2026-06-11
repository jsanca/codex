# Codex

Codex is a headless, multi-tenant, multi-language CMS conceived as a disciplined modular monolith.

It is not intended to be a traditional page-centric CMS. The center of Codex is the domain: structured content, revision-aware lifecycle, workflow-driven operations, and extensibility points that allow the system to evolve without bloating the core.

The Maven/Jigsaw modular foundation is established. Implementation is progressing incrementally — hardening the core domain model before expanding outward into exposure, persistence, and workflow layers.

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
- **Custos** is the authorization boundary. It models actors, permissions, resource refs and scopes, roles, role assignments, permission grants, built-in roles, and permission resolution. Custos authorizes domain operations — not HTTP endpoints — and remains authentication-agnostic.
- **Imaginarium** is the AI integration layer.
- **Olórin** is the agent layer.

This separation is intentional.

Imaginarium is not the domain agent itself. It exists to provide the infrastructure required for AI integration, orchestration, and interoperability. Olórin is the component responsible for reasoning over the Codex domain, planning meaningful actions, and operating through explicit capabilities.

## Current Phase

Codex is moving from architectural foundation into early domain implementation. The current priority is hardening the internal model before REST/API exposure or persistence.

Current focus:

- domain lifecycle (sites, content types, content items, revisions)
- domain events
- service decorators
- cache and index foundations
- Observance
- authorization via Custos
- auditability via Chronicon
- module boundary enforcement

Still deferred:

- REST/API exposure through Porta
- persistence strategy
- production authentication integration
- full workflow engine behavior
- step-up approval
- Olórin permission proposal execution

## Module Map

The current conceptual and technical modules are:

- `codex-bom`
- `codex-fundamentum`
- `codex-codex`
- `codex-chronicon`
- `codex-archivum`
- `codex-index`
- `codex-concilium`
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

- **Fundamentum** (`codex-fundamentum`) — Foundational shared abstractions and cross-cutting base types.
- **Codex** (`codex-codex`) — The central domain/kernel of the system.
- **Chronicon** (`codex-chronicon`) — History, revision memory, and publication narrative.
- **Archivum** (`codex-archivum`) — Storage abstraction layer.
- **Index** (`codex-index`) — Search and discoverability.
- **Concilium** (`codex-concilium`) — Runtime composition layer; assembles module runtimes.
- **Scriptorium** (`codex-scriptorium`) — Scripting and dynamic customization.
- **Illuminarium** (`codex-illuminarium`) — Enrichment and semantic enhancement.
- **Porta** (`codex-porta`) — External exposure layer, including APIs and integrations.
- **Iter** (`codex-iter`) — Workflow and orchestration.
- **Custos** (`codex-custos`) — Domain authorization: actors, roles, permissions, resource scopes, and resolution.
- **Imaginarium** (`codex-imaginarium`) — AI integration infrastructure.
- **Olórin** (`codex-olorin`) — Agent-oriented reasoning and domain-aware interaction.

## Build

This project uses **Maven** and **Java Modules (Jigsaw)**.

To build and test from the repository root:

```bash
mvn clean verify
```

## Documentation

Architectural notes, lore, specs, ADRs, and implementation roadmaps live in `docs/`.

This root README is intended to remain an entry point: concise, repository-oriented, and focused on helping readers understand what Codex is, how the repository is organized, and what phase of the project is currently in progress.

Module-specific intent and architectural details should live in the corresponding module documentation.

## Status

Codex is in early implementation. The modular structure is established. Core domain foundations — lifecycle, events, authorization, and auditability — are progressing deliberately. The system is not yet ready for production use.
