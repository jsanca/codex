# Codex

`codex-codex` is the central domain module of the system.

Its responsibility is to define what content is in Codex.

This module should contain the most essential domain concepts that make the rest of the platform possible. It is not the place for infrastructure concerns, persistence choices, or integration details. It is the semantic core of the manuscript world.

## Purpose

The purpose of this module is to answer the question:

**What is content in Codex?**

From that perspective, `codex-codex` owns the foundational content model of the platform.

## Owns

This module is the natural home for concepts such as:

- `Site`
- `ContentType`
- `ContentTypeVersion` or the equivalent versioned type model
- `ContentItem`
- `ContentRevision`
- editorial states
- content-oriented value objects
- content identities

Examples of value objects and identities may include:

- `SiteId`
- `ContentTypeId`
- `ContentItemId`
- `ContentRevisionId`
- `VariantGroupId`
- `Locale`
- `Slug`
- `EditorialState`

These types exist to give semantic meaning to concepts that would otherwise become raw strings, UUIDs, or generic maps.

## Does Not Own

This module should not become a dumping ground.

It does **not** own:

- persistence strategy
- JPA, JDBC, JSONB, or storage mappings
- REST controllers
- search integration
- asset storage concerns
- workflow engine implementation
- authorization rules
- scripting runtime
- AI or agentic behavior

Those concerns belong to other modules.

## Relationship to Other Modules

`codex-codex` should behave like the semantic trunk of the system.

Other modules may depend on it when they need to speak the language of content, but `codex-codex` itself should depend on as little as possible, ideally only on truly foundational shared abstractions.

A useful mental model is:

- **Codex** defines what content is
- **Chronicon** defines how it is remembered
- **Custos** defines who acts
- **Iter** defines how it advances
- **Archivum** defines how it is stored

## Identity Boundary

Users are expected to live in `codex-custos`, not in `codex-codex`.

However, the content model may still need lightweight references to actors such as:

- `createdBy`
- `lastModifiedBy`
- `publishedBy`

For that reason, this module may eventually depend on a minimal actor identity abstraction such as `ActorId`, `PrincipalId`, or a similarly small reference type, without taking ownership of the full identity and access-control model.

## Current Status

At this stage, this module is still part of the Phase 1 structural skeleton.

The goal for the next iterations is to progressively make this module the clean home of the core content language before deeper implementation decisions are introduced.