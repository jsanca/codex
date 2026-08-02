# Imaginarium Philosophy

Imaginarium exists so Codex can use AI deliberately without letting provider
details, model behavior, or agentic ambition leak into business domains.

It is the conceptual home for reusable AI infrastructure. It gives Codex a place
to reason about models, providers, contexts, capabilities, tools, skills,
supervision, and runtime behavior before any consuming agent platform decides what
to do with them.

## Why Imaginarium Exists

Codex needs AI infrastructure that can be shared by different future systems.

Some AI features will be simple: summarize a document, classify content, enrich
metadata, or generate a draft. Other AI features may become collaborative or
agentic: plan work, propose changes, coordinate workers, or evaluate mission
progress.

Those capabilities should not be scattered across business modules. They need a
shared conceptual foundation that is independent from any one provider, model,
framework, or agent.

Imaginarium provides that foundation.

## Problems It Solves

Imaginarium solves infrastructure-level AI problems:

* how Codex talks about AI providers and models
* how runtime context is assembled and bounded
* how capabilities, tools, skills, policies, and knowledge can be discovered
* how AI work can be grouped into missions and sessions
* how planning and supervision can be described without becoming business logic
* how future agentic systems can consume reusable AI infrastructure

It helps Codex avoid treating every AI feature as an agent.

It also helps Codex avoid coupling domain modules to model-specific or
provider-specific behavior.

## Problems It Intentionally Does Not Solve

Imaginarium does not own Codex business behavior.

It does not:

* own canonical content
* approve privileged actions
* bypass Custos authorization
* perform site, content type, or content item lifecycle decisions
* replace Illuminarium enrichment responsibilities
* replace Olorin or any future agentic platform
* define persistence, transport, or provider-specific implementation details

When a future system needs to decide what Codex operation should happen, that is
not Imaginarium acting as a business actor. Imaginarium supplies infrastructure.
The consuming domain or agentic layer owns the domain intent.

## Why AI Infrastructure Is Separate From Business Domains

Business domains should remain stable even when AI infrastructure changes.

Providers will change. Models will change. Prompting strategies will change.
Runtime context strategies will change. Supervision concepts will evolve.

Site lifecycle, content lifecycle, audit semantics, authorization semantics, and
canonical data ownership should not churn because an AI provider or agentic
pattern changes.

Separating AI infrastructure from business domains keeps Codex explainable:

* domain modules describe what Codex means
* Imaginarium describes how AI infrastructure can be used
* future agentic platforms describe how AI-assisted work is coordinated
* Custos remains the authorization gate

## Why Conceptual Clarity Comes First

Implementation-first design is risky in AI infrastructure.

If Codex starts with provider APIs, agent frameworks, or convenient class names,
the architecture may inherit accidental assumptions: one model shape, one tool
protocol, one orchestration style, or one idea of what an agent is.

Conceptual clarity gives Codex a better sequence:

* name the responsibility
* draw the boundary
* understand what it is not
* only then let APIs emerge

The language should be simple enough to teach before it becomes code.

## Guiding Principle

Discover the domain first.

Design the API second.

Write the code last.
