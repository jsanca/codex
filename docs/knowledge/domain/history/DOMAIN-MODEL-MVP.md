# Codex Domain Model (MVP)

This document defines the **canonical domain model** for Codex.

It is intentionally MVP-oriented:

- minimal concepts
- explicit invariants
- schemaless persistence formats (JSON/YAML)
- strong typing in the core

---

# 0. Canonical Cross-Cutting Fields

Many Codex entities share a minimal set of canonical fields.

## 0.1 Canonical Identifiers

- `id` — globally unique identifier (UUID recommended)
- `siteId` — the site/space scope for multi-tenancy (required for tenant-aware entities)

## 0.2 Canonical Metadata

- `status` — lifecycle status (entity-specific)
- `createdAt` — creation timestamp
- `updatedAt` — last update timestamp
- `author` — identity of the actor that created the entity
- `subject` — identity of the actor that last modified the entity (or last action actor)

## 0.3 Schemaless Attributes

- `attributes` — JSON map for extensible metadata that must not require schema migrations

> Note: "schemaless" is a persistence property.
> Semantics remain validated by the core model.

---

# 1. Identity, Users, Roles, Permissions

Codex is not an identity provider.

- **AuthN** is external (OIDC/OAuth2 for MVP; SAML later)
- **AuthZ** is internal (roles and permissions)

## 1.1 UserIdentity

Represents a user as seen by Codex.

**Fields**

- `id`
- `siteId` (optional if Codex supports global users; required if users are tenant-scoped)
- `username` (or `email`)
- `status` — `ACTIVE | DISABLED`
- `roles[]` — list of role identifiers
- canonical metadata: `createdAt`, `updatedAt`, `author`, `subject`
- `attributes` — optional JSON map

**Invariants**

- `username` must be unique within its scope (global or per site)
- a user may have zero or more roles

## 1.2 Role

**Fields**

- `id`
- `siteId` (same scoping rule as UserIdentity)
- `name`
- `permissions[]` — list of permission identifiers
- canonical metadata + `attributes`

**Invariants**

- role types/categories are not modeled in MVP

## 1.3 Permission

In MVP, permissions are identifiers.

Examples:

- `TYPE_CREATE`, `TYPE_EDIT`, `TYPE_VIEW`, `TYPE_DELETE`
- `CONTENT_CREATE`, `CONTENT_EDIT`, `CONTENT_VIEW`, `CONTENT_DELETE`
- `PUBLISH`, `UNPUBLISH`
- `SITE_ADMIN`

Codex may later introduce attribute-based policies, but MVP uses role-to-permission mapping.

## 1.4 Authorization Scope

Codex permissions are evaluated within a **site scope**.

For content:

- baseline permissions can be defined at Content Type level
- optional overrides can be defined at Content Item level

MVP intentionally avoids deep inheritance chains.

---

# 2. Sites

A Site is a first-class concept.

## 2.1 Site

**Fields**

- `id`
- `key` (human-friendly unique identifier)
- `name`
- `status` — `ACTIVE | DISABLED`
- canonical metadata + `attributes`

**Invariants**

- `key` must be unique globally

## 2.2 Reserved System Site

Codex supports a reserved site used to host shared resources (types, assets, defaults).

- `key` — `_system` (reserved)

The `_system` site is a normal `Site` instance from the domain perspective.

**Rule**

- Codex core must not implement "special-case" behavior for `_system`.
- `_system` exists to support resolution and fallback policies, not conditional logic scattered across the codebase.

## 2.3 Resource Resolution Policy (Fallbacks)

Many resources can be resolved with a simple fallback chain.

MVP policy:

1. Resolve in the current site scope (`siteId`)
2. If not found, resolve in the reserved `_system` site

This policy may apply to:

- Content Types
- Blueprints/recipes (CLI scaffolding)
- Assets (optional in MVP)

This allows shared defaults without requiring duplication across sites.

> Implementation note: model this as a dedicated resolver component (e.g., `ResourceResolver`) rather than embedding fallbacks in multiple services.

---

# 3. Content Model

Codex models content as structured knowledge.

## 3.1 ContentType

A Content Type defines a schema for Content Items.

### 3.1.1 Versioned Content Types

Content Types are versioned.

Codex treats schema changes as first-class and explicit.

**Fields**

- `id`
- `siteId`
- `key` (unique within site)
- `name`
- `description` (optional)
- `version` — integer (starts at 1)
- `status` — `DRAFT | ACTIVE | DEPRECATED`
- `fields[]` — list of FieldDefinition
- `composition` (optional) — composite definition (see 3.2)
- canonical metadata + `attributes`

**Invariants**

- `(siteId, key, version)` is unique
- only one version may be `ACTIVE` per `(siteId, key)` in MVP
- a Content Item must reference a specific Content Type version

### 3.1.2 Type-Level Permissions (Baseline)

A Content Type may define baseline access control.

MVP model:

- `acl` — a set of rules mapping roles to permissions for this type

This is the default policy applied to items unless overridden.

## 3.2 Composition (Composite Types)

Codex supports composite Content Types.

MVP rule: **composition is implemented by copy**.

Meaning:

- when creating a composite type, fields from referenced types are copied into the new type definition
- the composite stores its own FieldDefinitions
- changes to the source types do not implicitly alter the composite

**Benefits**

- predictable behavior
- avoids dependency/version resolution complexity in MVP

**Composite Metadata (optional)**

- `composition.sources[]` — list of `{ typeKey, typeVersion }` used as inputs

## 3.3 FieldDefinition

**Fields**

- `name` (unique within a type)
- `type` — `STRING | TEXT | NUMBER | BOOLEAN | DATE | DATETIME | JSON | REF | LIST`
- `required` — boolean
- `indexed` — boolean (advisory; may be ignored by some Index plugins)
- `unique` — boolean (MVP may support for simple types)
- `defaultValue` (optional)
- `constraints` (optional) — JSON map for type-specific rules
- `attributes` (optional)

**Invariants**

- field names must be unique within a Content Type version
- `REF` must specify a target type key (and optional version constraint) via constraints

## 3.4 ContentItem

A Content Item is an instance of a Content Type version.

**Fields**

- `id`
- `siteId`
- `typeKey`
- `typeVersion`
- `status` — `DRAFT | LIVE | ARCHIVED`
- `revision` — integer (starts at 1; increments on each saved change)
- `locale` (optional) — language variant marker (see 4)
- `variantGroupId` (optional) — links variants of the same logical content
- `data` — schemaless JSON map of field values
- `acl` (optional) — item-level overrides
- canonical metadata + `attributes`

**Invariants**

- a Content Item references an immutable Content Type version
- item `data` must validate against its Content Type schema
- only one `LIVE` item may exist per `(siteId, id, locale)` at a time

---

# 4. Language as a Variant, Not a Feature

Language is modeled as a content variant.

MVP strategy:

- each language variant is a separate Content Item
- variants are linked by `variantGroupId`
- `locale` identifies the language (e.g., `en-US`, `es-CR`)

**Invariants**

- all variants in the same `variantGroupId` share the same `typeKey` and `typeVersion`
- at most one `LIVE` per `(variantGroupId, locale)`

---

# 5. Publishing and Artifacts

Publishing produces a deployable artifact.

## 5.1 PublishRequest

**Fields**

- `siteId`
- `selector` — criteria for what becomes LIVE (MVP: all LIVE items)
- `requestedBy` — identity
- `requestedAt`

## 5.2 Bundle (Manuscript)

A bundle is a reproducible snapshot.

MVP structure:

- `manifest.json`
- `content-types.json`
- `content-items.json`
- `assets/` (optional)

### 5.2.1 Manifest

**Fields**

- `bundleId`
- `siteId`
- `createdAt`
- `createdBy`
- `contentTypeRefs[]` — list of `{ typeKey, typeVersion }`
- `checksums` (optional)

---

# 6. Events (Lifecycle Signals)

Codex communicates internally using events.

Events are used for:

- scripting hooks (Scriptorium)
- enrichment (Illuminator)
- audit trails
- integrations

## 6.1 Canonical Events (MVP)

### Content Types

- `ContentTypeCreated`
- `ContentTypeVersionActivated`
- `ContentTypeDeprecated`

### Content Items

- `ContentItemDraftSaved`
- `ContentItemPublished`
- `ContentItemArchived`

### Bundles

- `BundleGenerated`

## 6.2 Hook Mapping (Convenience)

Codex may expose hook-friendly aliases:

- `beforeSave` / `afterSave`
- `beforePublish` / `afterPublish`

Internally these map to the canonical events above.

---

# 7. Storage Implications (MVP)

Codex persists entities in a schemaless format.

## 7.1 Filesystem Storage Plugin (Archive-FS)

A recommended layout inspired by JCR-style paths (illustrative):

- `sites/<siteId>/site.json`
- `sites/<siteId>/users/<userId>.json`
- `sites/<siteId>/roles/<roleId>.json`
- `sites/<siteId>/types/<typeKey>/v<version>.json`  (e.g., `<siteKey>/types/page/v1.json`)
- `sites/<siteId>/items/<typeKey>/<itemId>/rev<revision>-<status>-<locale>.json`
- `sites/<siteId>/bundles/<bundleId>/...`

Filesystem persistence is an implementation detail; the core operates on typed objects.

---

# 8. Open Decisions (Tracked)

- ACL rule syntax (how roles map to permissions in a type/item)
- REF field constraints and version selection rules
- Unpublish semantics (LIVE → DRAFT? LIVE → ARCHIVED?)
- Bundle item selection strategies beyond "all LIVE"