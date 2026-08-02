# ADR-005: Intelligence Levels Are Defined by Responsibility Rather Than Model Size

## Status

Accepted as conceptual foundation. Terminology remains evolving.

## Context

It is tempting to classify AI agents by the size or strength of the model behind
them.

That classification is not architecturally useful enough for Codex. The safer and
clearer distinction is what responsibility the agent has in the system.

## Decision

Agent intelligence levels are defined by architectural responsibility rather than
model size.

Current conceptual categories include:

* deterministic agents
* reactive agents
* specialist agents
* planning agents
* supervisory agents

## Consequences

Model choice does not automatically determine authority, responsibility, or safety
posture.

Responsibility, policy, runtime boundaries, capabilities, and supervision matter
more than model size when describing an agentic role.

## Boundaries

This ADR does not assign specific models to categories and does not define product
tiers or implementation classes.
