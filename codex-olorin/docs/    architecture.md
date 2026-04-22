

# Olórin Architecture Vision

## Overview

Olórin is the agent layer of the Codex ecosystem.

Its role is not merely to provide access to language models, but to reason about the Codex domain, plan meaningful actions, and execute them through a guided and controlled interaction model.

In this architecture, Olórin is intentionally distinct from Imaginarum:

- **Olórin** is the agent.
- **Imaginarum** is the AI integration layer that provides connectors, model access, orchestration primitives, and interoperability with external AI tooling.
- **Codex CMS** is the operational domain where sites, content models, content items, assets, workflows, and related structures live.

This separation is deliberate. Codex should not become "agentic by default". Instead, Olórin is the system component that uses AI-enabled capabilities to reason and act over the Codex domain.

## Design Intent

Olórin should make it possible to interact with Codex using natural language in a way that is operationally meaningful.

Examples of desired interactions include:

- Create a site.
- Create a content type.
- Create a page-like content type.
- Create content items from a given content type.
- Create assets and attach them to content.
- Generate starter structures such as a pet store, blog, documentation site, or another blueprint-driven experience.

These interactions imply that Olórin must understand more than just entity persistence. It must understand:

- domain concepts,
- constraints,
- allowed operations,
- relationships between structures,
- and higher-level composition goals.

## Core Architectural Positioning

Olórin should not operate directly against raw persistence-oriented entities alone.

A user request such as:

> Create a pet store with a homepage, pet catalog, services page, contact page, demo assets, and sample content.

is not a single CRUD action. It is a composed objective that must be translated into a plan.

That means Olórin needs an intermediate operational layer capable of representing intent and execution.

Conceptually, such a request may expand into a plan like:

1. Create site.
2. Create required content types.
3. Create page-oriented content types.
4. Create demo content items.
5. Create or associate demo assets.
6. Create relationships and navigation structures.
7. Optionally publish or stage content.

## Required Layers

### 1. Domain Model

This layer contains the core business entities of Codex, such as:

- `Site`
- `ContentType`
- `ContentItem`
- `Asset`
- workflow-related concepts
- structural and navigational concepts

This layer answers: **what exists in the system?**

### 2. Capability and Descriptor Model

This layer describes what the system can do and how domain concepts should be interpreted.

It should include information such as:

- human-readable names,
- technical names,
- aliases,
- examples,
- constraints,
- defaults,
- field semantics,
- type capabilities,
- and operation descriptions.

This layer is important because Olórin must be able to reason about the domain in terms that are useful both to machines and humans.

For example, a `ContentType` should not only exist structurally. It should also be describable as something like:

- display name,
- aliases such as "content model" or "content type",
- purpose,
- required fields,
- whether it behaves like a page,
- whether it is routable,
- whether it supports workflow,
- and whether it is intended for composition.

This layer answers: **what does this concept mean, and what can be done with it?**

### 3. Command and Orchestration Model

This layer expresses domain-level intent and execution planning.

Examples include:

- `CreateSite`
- `CreateContentType`
- `CreatePageType`
- `CreateContentItem`
- `CreateAsset`
- `PublishContent`
- `InstantiateBlueprint`
- `GenerateStarterSite`

This layer should support the idea that Olórin does not mutate the system blindly. It plans actions, executes them through explicit domain commands, and records the results.

This layer answers: **how should the system act in response to an intent?**

## Blueprints

Blueprints are a key concept for making Olórin operational.

A request such as:

> Create a pet store.

should not require the agent to invent everything from scratch every time. Instead, the system should support reusable blueprint definitions that describe a composed solution.

A blueprint may define:

- required content types,
- page-oriented types,
- fields and relationships,
- sample content,
- sample assets,
- navigation structure,
- and starter configuration.

Examples of future blueprint families may include:

- pet store,
- blog,
- documentation portal,
- restaurant,
- event catalog,
- service directory.

Blueprints should be treated as structured, explainable, and extensible domain assets.

## Page Semantics

A particularly important modeling question is how to represent page-like content.

A page may be represented in one of two ways:

- as a specialized kind of content type,
- or as a general content type that exposes a specific set of capabilities.

The capability-oriented approach is more flexible.

For example, a content type may expose capabilities such as:

- routable,
- composable,
- workflow-enabled,
- template-backed,
- indexable,
- asset-aware.

This makes the model more expressive and gives Olórin clearer guidance about how a given type behaves.

## Explainability and Agent-Facing Semantics

Olórin needs access to human-friendly and machine-usable explanations of the domain.

This means the architecture should support descriptor-level metadata such as:

- technical identifier,
- display name,
- aliases,
- description,
- examples,
- validation hints,
- capability summaries,
- operation guidance.

These descriptors are useful not only for the agent, but also for:

- UI generation,
- documentation,
- API discoverability,
- system introspection,
- and external integrations.

## External Agent Discovery

One of the long-term goals of Codex is to allow external agents to discover what a Codex-powered site or system does, what services it offers, and how it can be interacted with.

An external agent should be able to ask questions such as:

- What is the purpose of this site?
- What services does this site provide?
- What capabilities are available here?
- What operations can be performed programmatically?
- How can I communicate with this system safely and meaningfully?

This requires Codex to become self-describing.

A traditional API is not sufficient on its own. The system should expose not only endpoints, but also purpose, semantics, capability definitions, and operational contracts.

## Semantic Exposure

For external semantic discoverability, Codex should support structured metadata publication.

A likely direction is to expose a semantic manifest using technologies such as:

- JSON-LD
- schema.org vocabularies where appropriate

This layer is useful for expressing:

- site identity,
- business purpose,
- offered services,
- event or product semantics,
- operational categories,
- discovery metadata for machine consumers.

This is especially valuable for public-facing systems where machine-readable meaning should be exposed at the web layer.

## Agent Protocol Exposure

In addition to semantic discoverability, Codex should support an operational interaction model for agents.

A likely direction is compatibility with patterns similar to MCP-style tool and resource exposure.

This means Codex may eventually expose:

- resources,
- tools,
- schemas,
- operation contracts,
- authentication requirements,
- and domain capability manifests.

In practical terms, this would allow an external agent to discover not only what a site is about, but also how to interact with it through well-described operations.

## Capability Manifest

To avoid hard-coding the architecture around a single external protocol too early, Codex should internally define a protocol-neutral capability manifest.

This manifest may later be serialized or adapted into:

- semantic web representations such as JSON-LD,
- agent-facing operational descriptors,
- MCP-compatible tool exposure,
- or other future integration formats.

A protocol-neutral capability manifest gives the architecture flexibility while preserving a single internal source of truth.

## Execution Traceability

Because Olórin operates through language and planning, execution should be traceable and explainable.

The architecture should support concepts such as:

- `ExecutionPlan`
- `ExecutionStep`
- `CommandLog`
- `ChangeSet`
- `OperationResult`
- `DomainEvent`
- reversible or compensating actions where appropriate

This is important for:

- auditability,
- explainability,
- debugging,
- undo or compensation flows,
- workflow history,
- and safe collaboration between humans and agents.

## Example Long-Term Scenario

A useful mental model for this architecture is the idea of an external assistant helping a person complete a real-world task while on the move.

For example, a user might say:

> Buy two tickets for me and my son for a children’s show, with a reasonable duration, and ending no later than a given hour.

In a future Codex-powered ecosystem, a system that publishes its purpose, event semantics, search capabilities, filtering options, reservation operations, and purchase flow in a machine-discoverable way would be much easier for an external assistant to understand and use.

This is one of the motivating examples for making Codex both human-friendly and agent-friendly.

## Summary

Olórin is not simply an AI helper attached to Codex.
It is the agent layer responsible for reasoning over the domain, planning structured actions, and operating safely through explicit capabilities.

To support that vision, the architecture should evolve around the following principles:

- clear separation between agent, AI infrastructure, and core domain,
- protocol-neutral capability modeling,
- blueprint-driven composition,
- explainable descriptors and semantics,
- explicit command and orchestration layers,
- self-describing discovery for external agents,
- and traceable, auditable execution.

This makes Codex not only a CMS operable by humans, but a system that can eventually describe itself, collaborate with agents, and participate in a broader machine-readable ecosystem.