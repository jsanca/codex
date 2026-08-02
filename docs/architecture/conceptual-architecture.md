# Imaginarium Conceptual Architecture

This document explains the current conceptual layers around AI in Codex.

It is not an API design. It is not an implementation plan. It avoids provider,
framework, and language-specific assumptions.

## Current Layering

The emerging architecture separates application behavior, enrichment behavior, AI
infrastructure, and provider access.

```text
Application
    ↓
Illuminarium
    ↓
Imaginarium
    ↓
Providers
```

## Application

The application layer owns product intent.

It decides why AI is being used in a user-facing or domain-facing workflow. It may
ask for summarization, enrichment, proposal generation, review support, or
collaboration.

The application layer does not need to know provider implementation details.

## Illuminarium

Illuminarium is the enrichment and semantic enhancement layer.

It can consume Imaginarium when it needs AI infrastructure for content analysis,
classification, summarization, metadata extraction, semantic annotations, or
enrichment projections.

Illuminarium justifies itself because not every AI feature needs an agent.
Many useful AI capabilities are domain services or enrichment workflows, not
autonomous systems.

## Imaginarium

Imaginarium is AI infrastructure.

It owns the conceptual foundation for:

* provider abstraction
* model abstraction
* runtime context
* capabilities
* tools
* skills
* policies
* knowledge
* registries
* planning infrastructure
* supervision infrastructure

Imaginarium does not decide Codex business behavior. It does not own canonical
content. It does not bypass Custos.

## Providers

Providers are sources of AI model access or AI-related infrastructure.

They are below Imaginarium in the conceptual stack because they should not shape
business domains directly.

Providers supply capability. They do not define Codex semantics.

## Future Agentic Platforms

Future agentic platforms are expected to consume Imaginarium rather than be part
of it.

Olorin is one future domain-aware agent/planner concept, but Olorin should not be
treated as the framework itself.

An intermediate Agent Platform has emerged conceptually and is still under
discovery. That platform would sit above Imaginarium and use Imaginarium's
runtime, context, capability, tool, skill, policy, knowledge, model, and provider
concepts.

The exact name and shape of that platform remain provisional.

## Conceptual Shape

```text
Application
    ↓
Domain-specific AI behavior
    ↓
Illuminarium or future Agent Platform
    ↓
Imaginarium
    ↓
Providers
```

This keeps Imaginarium reusable. It can serve enrichment systems, collaborative
systems, and future agentic systems without becoming any one of them.
