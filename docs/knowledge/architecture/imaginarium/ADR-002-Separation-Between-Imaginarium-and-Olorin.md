# ADR-002: Separation Between Imaginarium and Olorin

## Status

Accepted as conceptual foundation.

## Context

Earlier discussions associated several agent runtime concepts with Olorin. Further
review clarified that many of those concepts are generic AI runtime infrastructure,
not Olorin-specific business behavior.

Olorin is expected to become a domain-aware agent or planner. Imaginarium is the AI
infrastructure space.

## Decision

Imaginarium and Olorin remain separate.

Imaginarium owns reusable AI infrastructure concepts. Olorin may later consume that
infrastructure for Codex-aware planning, proposals, and domain-specific assistance.

## Consequences

Concepts such as runtime lifecycle, session management, context composition,
provider abstraction, model abstraction, registries, planning infrastructure, and
supervision infrastructure belong to Imaginarium.

Codex-specific agent behavior, permission proposal flows, or domain-aware planning
belong to Olorin or another future domain-specific agent layer.

## Boundaries

This ADR does not implement Olorin and does not define Olorin's execution model.
It only protects the architectural separation.
