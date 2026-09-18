# ADR-001: Imaginarium Owns AI Infrastructure

## Status

Accepted as conceptual foundation.

## Context

Codex needs a place for reusable AI infrastructure concepts such as model access,
provider abstraction, runtime context, capability exposure, registries, planning
support, and supervision support.

These concepts are not business behavior. They are infrastructure for future
AI-assisted systems.

## Decision

Imaginarium owns the generic AI infrastructure foundation for Codex.

It may define the conceptual language for:

* Agent Runtime
* context composition
* model abstraction
* provider abstraction
* registries
* planning infrastructure
* supervision infrastructure
* reusable capabilities and skills

## Consequences

Imaginarium remains separate from canonical content ownership and domain lifecycle
rules.

Future agent layers may consume Imaginarium infrastructure without forcing
Imaginarium to become the agent layer itself.

## Boundaries

This ADR does not define Java APIs, implementation classes, persistence, provider
adapters, or a roadmap.
