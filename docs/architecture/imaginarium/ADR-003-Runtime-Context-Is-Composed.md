# ADR-003: Runtime Context Is Composed of Multiple Specialized Contexts

## Status

Accepted as conceptual foundation.

## Context

The term "memory" is too broad for the Agent Runtime. It can hide very different
responsibilities behind a single word.

Conversation history, personality, role, policy, user information, mission
direction, capabilities, and temporary working notes have different meanings and
different risks.

## Decision

Runtime context is composed of multiple specialized contexts.

Current conceptual context types include:

* conversation context
* personality context
* role context
* capability context
* policy context
* user context
* mission context
* working context

## Consequences

Future designs should state which kind of context they are using instead of calling
everything memory.

Memory remains a useful term only when retention is explicitly part of the design.
Knowledge remains a useful term for semantic grounding, but it is not a substitute
for context taxonomy.

## Boundaries

This ADR does not define persistence, retrieval, token selection, storage
mechanisms, or Java APIs.
