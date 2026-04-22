

# Codex CMS — MVP Spec

## 1. What Codex is

Codex is a headless, multi-tenant, multi-language CMS designed as a disciplined modular monolith.

Its purpose is not only to store content, but to manage structured knowledge through a strongly typed domain model, rich revision history, workflow-driven operations, and extensibility points that allow the platform to evolve without bloating the core.

Codex is not envisioned as a traditional page-centric CMS. The UI is not the center of the system. The center is the domain, its contracts, and the operational flow that governs content lifecycle.

---

## 2. Architectural stance

### 2.1 Modular monolith

Codex will start as a modular monolith with clear internal boundaries.

Reasons:
- avoid premature microservices complexity
- preserve transactional consistency where it matters
- keep development speed high during the early phases
- allow future extraction of modules if the domain proves it necessary

Modules must be separated by contracts and responsibilities, not only by packages.

### 2.2 External interfaces

The primary external interface for Codex in the MVP is REST.

REST is the public contract exposed by sites and services.

If the monolith is ever split in the future, internal communication between extracted services may use gRPC, but this is not part of the MVP.

### 2.3 Deployment stance

Codex is conceived as a standalone Java application.

Primary deployment model:
- normal Java application
- official Docker image
- container-first operational model

Future optional packaging:
- jlink
- native image
- binary distribution

These are not part of the first MVP.

---

## 3. Core domain

Codex revolves around a typed content domain and a workflow-driven operational model.

The core is not only content and not only workflow. It is the combination of:
- structured content as a first-class domain
- workflow as the execution and traceability backbone

### 3.1 First-level entities

The initial first-level entities of the system are:
- `Site`
- `User`
- `Role`
- `Permission`
- `ContentType`
- `ContentItem`
- `ContentRevision`
- `WorkflowDefinition`
- `WorkflowInstance`
- `Bundle` or publication artifact
- domain events / chronicle records

### 3.2 Multi-tenant model

Codex is multi-tenant.

A `Site` is a first-class entity and not a cosmetic partition. A site defines an isolated operational scope for content, permissions, workflow execution, configuration, and publication.

### 3.3 System tenant

Codex includes a special tenant called `_system`.

The `_system` site is used for global resources that should still belong to a site-like scope rather than being treated as special-case logic scattered throughout the code.

Examples of resources that may live in `_system`:
- system user
- system roles
- global content types
- shared definitions
- future global layouts or templates

The exact scope of layouts/templates is not frozen in the MVP, but `_system` exists from the beginning as a first-class tenant.

---

## 4. Permissions and authorization

### 4.1 General direction

The MVP will not implement an overly sophisticated ACL model.

The initial authorization approach should be simple, explicit, and future-extensible.

The short-term direction is:
- RBAC as the base
- possibility of future overrides
- careful design so that the authorization model can evolve without breaking the domain

### 4.2 Main authorization entities

The initial model includes:
- `Role`
- `Permission`
- `RoleAssignment`

These should be abstract enough to allow future customization.

### 4.3 Permission scopes

Permissions may apply to scopes such as:
- system
- site
- content type
- content item
- workflow action

### 4.4 Initial actions

Initial actions may include:
- `read`
- `create`
- `update`
- `delete`
- `publish`
- `archive`
- `manage_type`
- `execute_action`
- `administer_site`

`execute_action` intentionally remains generic in the MVP.

Reasoning:
In the long term, workflow actions may map to concrete CMS operations such as check-out, save, publish, archive, etc. The initial model should avoid premature granularity while staying well designed for later refinement.

### 4.5 Site permission caution

Permission checks involving `Site` must be designed carefully.

One known design risk from older CMS systems is that content becomes inaccessible due to awkward combinations of site-level and content-type-level permissions.

The MVP should prefer a simpler and more predictable rule model, even if it is less feature-complete at first.

---

## 5. Content model

### 5.1 Structured knowledge

A `ContentItem` is not an arbitrary JSON blob.

Every content item must conform to a `ContentType`, and `ContentType` is versioned.

This gives Codex:
- structural consistency
- semantic clarity
- safer evolution of schemas over time
- traceability between content and its defining type version

### 5.2 Content type versioning

`ContentType` is versioned from the beginning.

A content item is associated with the specific content type version against which it was validated or created.

This is essential to avoid a model where all content is implicitly bound to the latest mutable schema.

### 5.3 Separation of logical item and revision

The MVP adopts a clean separation between:
- `ContentItem`: the logical identity of the content
- `ContentRevision`: a concrete snapshot of that content at a point in time
- `PublishedPointer`: a reference to the current live revision

This separation is preferred over a single “god entity” that tries to represent identity, draft state, live state, history, and metadata all at once.

### 5.4 Suggested model

#### `ContentItem`
Represents the stable logical identity of the content.

Suggested responsibilities:
- item identity
- site ownership
- content type reference
- locale / variant identity
- stable metadata
- lifecycle summary metadata if needed

#### `ContentRevision`
Represents a concrete version of the content payload.

Suggested responsibilities:
- revision number
- payload snapshot
- creation metadata
- workflow context if needed
- editorial state

#### `PublishedPointer`
Represents the currently published revision for a content item.

This may be implemented as a dedicated structure or as explicit references maintained by the item lifecycle service.

---

## 6. Revision lifecycle

### 6.1 Motivation

Codex should not model publication and revision lifecycle as a loose set of ambiguous flags.

The revision lifecycle should be explicit and model-driven.

### 6.2 Semantics

Recommended semantics for the MVP:
- editing creates or updates a draft revision
- publishing promotes a specific revision to become the live revision
- only one live revision may exist per content item variant at a time
- previous live revisions remain preserved as historical revisions
- archiving removes the content from public delivery without deleting history
- rollback may be implemented later as republishing a previous revision

### 6.3 Conceptual states

Conceptually, the system distinguishes:
- `DRAFT`
- `LIVE`
- `ARCHIVED`

The exact storage representation may vary, but the semantic model must remain explicit.

### 6.4 Item vs revision

A future refinement may distinguish more clearly between:
- item-level lifecycle status
- revision-level editorial state

For the MVP, the important decision is that history and publication are revision-aware, not only flag-based.

---

## 7. Multi-language model

### 7.1 General approach

Codex is multi-language.

Language is modeled as a variant of the content.

### 7.2 Chosen direction

The preferred direction for the MVP is:
- one `ContentItem` represents one language variant
- related language variants are grouped by a shared `variantGroupId`

This means that a conceptual content entry may be represented by multiple items, one per locale.

### 7.3 Why this direction

Advantages:
- each language can have its own lifecycle
- each language can have its own revisions
- publishing by locale becomes natural
- one translation may lag behind another without polluting the model
- fallback rules remain external to the core content identity

### 7.4 Variant metadata

Because variant identity should not become a vague ad-hoc attribute, the model should encapsulate variant-related data explicitly.

This may be represented through a dedicated metadata structure such as:
- locale
- variant group id
- fallback locale if applicable
- default variant marker if needed

Whether this is implemented as direct attributes or through a `ContentMetadata`-like structure is an internal design choice, but the model must remain explicit.

---

## 8. Publication model

### 8.1 Publish is not just a flag

Publication should not be reduced to a simple `live=true` flag.

The MVP already adopts the idea that publication promotes a concrete revision of a logical content item.

### 8.2 Bundle / publication artifact

Codex should preserve the idea of publication artifacts or bundles.

Even if the first implementation is minimal, publication should conceptually move toward:
- reproducibility
- transportability
- auditability
- separation between editing state and delivery state

The exact final publish semantics are still open for deeper refinement, but the MVP keeps publication revision-aware and artifact-oriented.

---

## 9. Workflow model

### 9.1 Workflow as backbone

Workflow is part of the core identity of Codex.

The long-term vision includes:
- steps
- conditional branches
- loops
- blocking waits
- external feedback
- background tasks
- batching
- streaming
- merge-like behaviors

### 9.2 MVP reduction

The MVP must reduce this ambition into a smaller executable subset.

Initial workflow support should focus on:
- workflow definitions
- workflow instances
- action execution
- transition history
- mapping workflow actions to CMS operations

### 9.3 Logic and scripting

Future-forward extensibility should allow workflow logic and custom behaviors to be expressed through JavaScript rather than forcing every extension to be a fully compiled Codex module.

Candidate direction:
- JavaScript-based extension logic
- GraalJS or equivalent runtime
- scripts stored either inline, hardcoded, or referenced from the virtual filesystem

This is a strategic direction, but the exact scripting runtime scope for the MVP is still to be determined.

---

## 10. History, chronicle, and events

Codex should preserve a rich operational history.

It is not enough to record that a field changed.

Codex should evolve toward a chronicle that can capture:
- what happened
- who triggered it
- on which entity
- through which action or workflow
- at what time
- with what resulting transition or consequence

Events are first-class extensibility points, not only internal logs.

This history model may begin small in the MVP, but it must be designed as a durable architectural concern.

---

## 11. Extensibility and customization

### 11.1 Direction

The main concern is not “plugins” as a heavy deployment mechanism, but extensibility and customization.

The platform should be extensible through:
- hooks
- events
- scriptable behaviors
- internal extension points

### 11.2 JavaScript extensions

A preferred future direction is that custom extension logic can be implemented in JavaScript, even though the platform core is written in Java.

Use cases may include:
- workflow logic
- hooks
- custom actions
- calculated behavior
- script-driven policies

### 11.3 Script storage

Scripts may eventually be:
- embedded
- stored in a virtual filesystem
- referenced by workflow definitions or hooks

This remains a key design direction even if the MVP starts with a smaller scripting surface.

### 11.4 Observability for developers

Developer-facing observability is an important part of extensibility.

Codex should expose enough operational visibility so that customizations and lifecycle execution can be understood and debugged.

---

## 12. Technology stack for the MVP

### 12.1 Application framework

The selected application framework is:
- Spring Boot, used in a disciplined way

The intent is to gain productivity and ecosystem maturity without surrendering architectural clarity.

### 12.2 Primary database

The primary source of truth is PostgreSQL.

PostgreSQL may also use `pgvector` initially where useful, but the design must remain flexible enough to evolve if vector scale or architecture later demands a dedicated approach.

### 12.3 Search pipeline

Search should follow a hybrid direction.

Initial stack:
- PostgreSQL as transactional source of truth
- OpenSearch for lexical search and search-oriented indexing
- optional vector support through PostgreSQL/pgvector initially, with flexibility to evolve later

### 12.4 Assets

Assets should use object storage as the source of truth.

Initial direction:
- S3 or S3-compatible storage as source of truth
- local transient node copy allowed for operational convenience
- if local copies are lost, they can be restored from object storage

A future chain-like retrieval strategy may be used, but object storage remains authoritative.

### 12.5 Caching

Initial cache stack:
- Caffeine for local cache
- Valkey for distributed cache
- event-driven invalidation

This is the preferred MVP direction.

---

## 13. Storage notes

If a filesystem-based storage model is used for some internal structures, the implementation must take into account practical filesystem limits such as directory fan-out.

This means generated paths should avoid placing too many files under the same directory and may use sharding strategies such as path prefixes derived from item identifiers.

This concern must be reflected in any local archive or virtual filesystem implementation.

---

## 14. What the MVP includes

The MVP should include at least:
- modular monolith structure
- system tenant and site model
- users, roles, permissions base model
- versioned content types
- content items and content revisions
- locale variants grouped by `variantGroupId`
- explicit draft/live/archive lifecycle semantics
- revision-aware publish model
- basic workflow definitions and execution model
- REST API as the primary interface
- PostgreSQL-backed transactional model
- OpenSearch integration for lexical search
- S3-compatible asset storage integration
- local + distributed cache strategy
- event and hook foundation

---

## 15. What is intentionally left open

The following areas are acknowledged but not fully frozen yet:
- exact publish artifact mechanics
- precise scripting runtime scope in the MVP
- exact richness of workflow nodes in the first implementation
- final layout/template model
- full permission override semantics
- whether certain metadata belongs directly on `ContentItem` or in a dedicated metadata structure
- how far vector search goes in the first release

These should be resolved through focused ADRs and iterative implementation, not through accidental framework defaults.