# Annex: Speculum — The Mirror Layer of Codex

## 1. Purpose

**Speculum** is the Codex layer responsible for reflecting the canonical content model onto external work, synchronization, or collaboration surfaces.

Its purpose is not to replace the central Codex model, nor to turn an external system into the CMS's source of truth. Speculum exists to allow Codex knowledge units to appear, be edited, organized, or synchronized in spaces familiar to humans and teams, such as:

*   Local filesystem
*   Google Drive
*   Google Docs
*   Dropbox
*   OneDrive
*   Git
*   S3-like storage

Speculum is not the CMS.  
Speculum is the human and operational mirror of the CMS.

> **Speculum does not copy files; it reflects knowledge.**

---

## 2. Motivation

In many real-world editorial workflows, people do not write directly within a CMS. It is common for marketing, documentation, product, or business teams to work first in tools like Google Docs, Word, shared folders, or collaborative documents.

The traditional flow usually looks like this:

```text
Google Docs / Word / Drive
    ↓
manual copy-paste
    ↓
CMS
    ↓
publication
```

This flow introduces several problems:
*   Duplication of work
*   Loss of formatting or editorial intent
*   Divergent versions
*   Manual errors
*   Loss of context
*   Difficulty to automate
*   Poor integration with agents

Codex should not initially compete against mature editors like Google Docs. Instead, it must be able to integrate them as editorial surfaces or mirrors.

The opportunity lies in allowing people to continue working where they already work, while Codex preserves the semantic structure, identity, versions, workflows, and content projections.

---

## 3. Architectural Principle

Speculum is based on a fundamental principle:

> **Codex Core is canonical.**  
> **Speculum is a projection.**  
> **Adapters are replaceable.**

Codex Core maintains the semantic truth. Speculum generates and consumes reflections. Adapters materialize those reflections in concrete systems.

Google Drive, Dropbox, OneDrive, Git, or the local filesystem must not define the Codex model.

*   They are views.
*   They are surfaces.
*   They are reflections.

They are **not** the semantic source of truth.

---

## 4. Relationship with the Canonical Model

Codex manages entities such as:
*   Site
*   ContentType / ContentTypeVersion
*   ContentItem / ContentRevision
*   Field / Asset
*   Workflow state
*   Events / Permissions
*   Knowledge Units

Speculum can reflect these entities as navigable structures:

```text
/sites
  /portfolio
    site.codex.json

    /content-types
      /article
        type.codex.json

    /content
      /article
        /codex-speculum
          content.codex.json
          body.codex.json
          body.gdoc-link

    /assets
      /images
        hero.png
```

However, this structure is not the domain. It is an **external representation** of the domain. The core should not depend on paths, folders, filenames, Google Drive IDs, or specific provider details.

---

## 5. Speculum as a Projection Layer

The conceptual relationship can be visualized as:

**Codex canonical model** $\rightarrow$ **Speculum projection** $\rightarrow$ **Mirror adapter** $\rightarrow$ **Target (GDrive/Local FS/etc.)**

This allows the same content to have multiple reflections simultaneously:
*   Local filesystem mirror
*   Google Drive editorial mirror
*   Git versioned mirror
*   S3 asset mirror

For example, an article could exist canonically as a `ContentItem`, but be reflected in:
*   `/content/articles/codex-speculum/content.codex.json`
*   `/content/articles/codex-speculum/body.codex.json`
*   Google Doc linked as editorial body
*   Git export for versioned publication
*   Static site output after rendering

---

## 6. Multiple Simultaneous Mirrors

Codex must be able to support several mirrors for the same knowledge unit.

**Conceptual Example:**

```json
{
  "contentId": "article:codex-speculum",
  "mirrors": [
    {
      "kind": "local-fs",
      "path": "/sites/portfolio/content/articles/codex-speculum",
      "role": "developer-workspace"
    },
    {
      "kind": "google-drive",
      "resourceId": "drive-folder-123",
      "role": "editorial-workspace"
    },
    {
      "kind": "git",
      "path": "content/articles/codex-speculum",
      "role": "versioned-export"
    }
  ]
}
```

Each mirror can have different rules: *read-only, write-only, pull-push, editorial-owner, canonical-owner, asset-only, metadata-only.*

---

## 7. PathResource and Semantic Metadata

In Speculum, a path should not be understood as a simple file path. A `PathResource` must have sufficient metadata for Codex to discover what it represents, how to treat it, and which entity it belongs to.

**Conceptual Example:**

```json
{
  "resourceKind": "mirror-content-item",
  "adapter": "google-drive",
  "siteKey": "portfolio",
  "contentType": "article",
  "contentKey": "codex-speculum",
  "field": "body",
  "syncMode": "pull-push",
  "canonicalOwner": "codex",
  "editorialOwner": "google-docs"
}
```

This allows Codex to interpret a folder or document not as an isolated file, but as part of a knowledge unit.

---

## 8. Google Drive as an Adapter, Not a Core Dependency

Google Drive may be a significant first implementation, but it must not constrain the architecture. Drive must be modeled as an adapter (`GoogleDriveSpeculumAdapter`), not as part of the core.

The same logic applies to:
*   `LocalFileSystemSpeculumAdapter`
*   `DropboxSpeculumAdapter`
*   `GitSpeculumAdapter`

Codex should not assume Google Drive behaves like a traditional filesystem. Drive has its own IDs, permissions, duplicate names, and ownership rules that should not leak into the domain.

---

## 9. Google Docs as an Editorial Surface

Google Docs can serve as an editorial surface for rich content fields, especially long bodies like:
*   `article.body`
*   `chapter.body`
*   `documentation.body`

However, Google Docs is not the complete truth of the content. It is the **temporary editorial truth** of a specific field, while Codex preserves the identity, type, required fields, workflow, and permissions.

---

## 10. Relationship with Semantic Content

Speculum is not limited to reflecting pages. Codex manages **Knowledge Units**, which can represent:
*   Articles / Book chapters
*   Emails / ADRs
*   Services / Products
*   Contracts / Internal notes

**"A page is only one projection of a knowledge unit."**

---

## 11. Difference between Mirror, Renderer, Indexer, and Agent Descriptor

| Category | Function | Examples |
| :--- | :--- | :--- |
| **Mirror** | External sync/work surface | GDrive, Local FS, Git |
| **Renderer** | Presentational output | HTML, React, PDF, Markdown |
| **Indexer** | Search/Retrieval structures | Lexical index, Embeddings, Graph |
| **Agent Descriptor** | Description for AI Agents | Business summary, Interaction rules |

---

## 12. Relationship with Projection Pipelines

Speculum can feed or consume projection pipelines, but it is not a renderer. Publication projections (like static HTML) should be generated from the **Codex canonical model**, not from the mirror filesystem.

---

## 13. Potential Conceptual API

```java
public interface SpeculumAdapter {
    SpeculumScanResult scan(SpeculumLocation location);
    SpeculumWriteResult reflect(SpeculumProjection projection, SpeculumLocation location);
}

public record SpeculumLocation(String adapter, String uri) {}

public record SpeculumProjection(SiteKey siteKey, List<SpeculumResource> resources) {}

public enum SpeculumResourceKind {
    SITE_MANIFEST, CONTENT_TYPE_MANIFEST, CONTENT_ITEM_MANIFEST, CONTENT_FIELD, ASSET, LINK
}
```

---

## 14. First Recommended Adapter

The first adapter should **not** be Google Drive. It should be the **LocalFileSystemSpeculumAdapter**.

*   **Reasons:** Simpler implementation, easier to test, no OAuth, no quotas, no remote permissions. Ideal for TDD and understanding the mirror structure.

---

## 15. Risks and Pending Decisions

Speculum introduces challenges that must be addressed:
*   Sync conflicts and ownership.
*   External vs. internal permissions.
*   Folder renaming and resource movement.
*   Accidental deletion and locks.

**Initial Rule:** Codex owns identity and schema. External mirrors may own editable field bodies or assets. Conflicts must produce explicit `SyncConflict` records.

---

## 16. Recommended Decision

Speculum must be implemented as a module separate from the core.
*   `codex.speculum.api` (Depends on Codex Core API)
*   `codex.speculum.internal` (Projection logic)
*   `codex.speculum.local` / `codex.speculum.drive` (Adapters)

---

## 17. Guiding Phrases

*   Speculum is not the CMS; it is the human reflection of the CMS.
*   Drive is a view, not the truth.
*   Speculum does not copy files; it reflects knowledge.
*   Codex Core maintains semantic truth; Speculum materializes operable mirrors.

---

## 18. Summary

Speculum allows Codex to dialogue with the external world without losing semantic independence. It exists because Codex should not force people to abandon their tools; it should turn those tools into gateways to a semantic business content system.

**Codex preserves knowledge. Speculum shows its reflection.**