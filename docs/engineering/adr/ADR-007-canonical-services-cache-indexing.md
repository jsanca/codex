# ADR-007: Codex Experience Packs as External Knowledge and Rendering Units

## Status

Future Forward

## Context

Codex produces semantic knowledge units as JSON node trees. This model is conceptually correct and consistent with the broader Codex architecture: Codex owns the canonical semantic representation of content, but it does not own a specific frontend, rendering engine, template language, or visual presentation model.

However, there is a pragmatic gap: users often do not want only a semantic tree. They want a real website, application screen, landing page, static site, storefront, or other consumable digital experience.

Codex should not become coupled to HTML, CSS, React, Node.js, templating engines, static-site generators, or any particular frontend rendering pipeline. Rendering and presentation concerns must remain outside the core.

At the same time, Codex needs an ecosystem unit that can bridge the gap between canonical semantic content and concrete user-facing output.

Earlier drafts used the word **Theme** as a metaphor. That term is too narrow because it suggests a visual skin. The intended concept is broader: a package of domain knowledge, content modeling assumptions, rendering guidance, tools, and optional agent instructions.

This ADR therefore uses the term **Experience Pack**.

An Experience Pack is not merely a UI theme. It is an external unit of knowledge and rendering capability that consumes Codex through public APIs.

Codex will also include an agent, **Olorin**, capable of materializing content types, content items, and site structures from user intent. Olorin can use Experience Packs as specialized context for different domains: corporate sites, blogs, e-commerce, landing pages, documentation portals, portfolios, and other digital experiences.

---

## Decision

Codex will support external **Experience Packs**.

An Experience Pack is an autonomous, self-contained unit that may include:

1. **Content type blueprints** for a specific use case.
2. **Sample or starter content** where appropriate.
3. **Presentation hints** that guide rendering without forcing Codex to understand frontend logic.
4. **A rendering or projection pipeline** that transforms Codex semantic trees into concrete artifacts.
5. **A renderer/proxy**, such as a Node.js server, that can act as a reverse proxy:
   - receives requests,
   - checks cache,
   - calls Codex APIs,
   - retrieves semantic trees,
   - runs the rendering pipeline,
   - returns HTML or another rendered artifact.
6. **Static-site generation support**, where the pipeline runs at build time and writes artifacts to disk.
7. **Olorin-specific instructions or tools**, giving the agent domain-specific behavior when creating or managing content.

Codex itself does not install, execute, or understand Experience Packs as internal concepts.

Codex only stores canonical domain artifacts created through public APIs:

- Sites
- Content types
- Content type versions
- Content items
- Content revisions
- Semantic page trees
- Metadata and presentation hints as ordinary content data

The coupling between an Experience Pack’s blueprints and its rendering pipeline is intentional, explicit, and contained inside the Experience Pack. It must not leak into the Codex core.

---

## Rationale

### Codex remains the semantic source of truth

Codex owns the canonical semantic model of the content. It does not own the final visual representation.

The Experience Pack interprets Codex content and presentation hints to produce a concrete output. This keeps Codex stable, backend-oriented, and frontend-agnostic.

### Experience Packs are broader than visual themes

A traditional CMS theme usually means templates, CSS, and assets.

A Codex Experience Pack is broader. It may include:

- content modeling blueprints,
- domain-specific conventions,
- rendering logic,
- Olorin instructions,
- cache rules,
- static generation rules,
- proxy behavior,
- transformation pipeline steps.

This makes the Experience Pack a product-level unit rather than a visual skin.

### The unavoidable coupling is contained

There is an unavoidable relationship between:

```text
content type blueprint → semantic tree shape → rendering pipeline
```

This coupling is not a flaw. It is a pragmatic necessity.

The key decision is that this coupling lives outside Codex, inside the Experience Pack. Codex remains unaware of the rendering assumptions.

### Pragmatism over excessive abstraction

Systems such as Apache Cocoon demonstrated the power of transformation pipelines, but also the risk of making the model too abstract and difficult for users to understand.

Codex should keep the golden path simple:

```text
semantic page → Experience Pack pipeline → rendered output
```

Advanced users may customize or replace pipeline steps, but ordinary users should not need to understand a complex graph of transformations.

### Olorin as the bridge

Olorin can operate at the level of abstraction Codex requires. Users do not need to manually reason about content type schemas, semantic trees, projection pipelines, or rendering hints.

An Experience Pack gives Olorin domain-specific context:

```text
"Build a small corporate website"
"Build a product landing page"
"Build a simple e-commerce catalog"
"Build a portfolio"
"Build a documentation site"
```

The Experience Pack can teach Olorin what content types to create, what fields matter, what pages are expected, and what rendering conventions the pipeline supports.

---

## Conceptual Model

An Experience Pack may contain:

```text
Experience Pack
├── manifest
├── blueprints
│   ├── content-types
│   ├── sample content
│   └── page structures
├── presentation-hints
├── rendering pipeline
│   ├── semantic enrichment slug
│   ├── validation slug
│   ├── transformation slug
│   └── renderer slug
├── renderer/proxy
├── static-site generator
├── cache strategy
└── Olorin instructions/tools
```

The exact packaging format is future work.

---

## Rendering Pipeline

The rendering pipeline inside an Experience Pack is a linear chain of replaceable pipeline steps, called slugs.

Each slug should have:

* a clear name,
* a version,
* a typed input contract,
* a typed output contract,
* a bounded responsibility,
* a clear failure policy,
* optional cacheability hints,
* optional observability hooks.

The default model is linear:

```text
semantic tree → enrich → validate → transform → render
```

Advanced fan-out is allowed only inside explicit producer slugs.

For example, a static-site generation slug may receive one semantic page node and emit multiple artifacts:

```text
/about/index.html
/about/mobile.html
/about/metadata.json
```

The pipeline remains conceptually linear. The fan-out is localized inside the producer slug, which owns the naming convention of the artifacts it emits.

Codex does not know or care that this fan-out happened.

---

## Rendering Modes

Experience Packs may support one or more rendering modes.

| Mode                              | Description                                                                                                       |
| --------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| SPA consumption                   | The semantic tree is consumed directly by a frontend application as props, store data, or API payload.            |
| Traditional server-rendered pages | A proxy/renderer receives a request, retrieves the semantic tree from Codex, runs the pipeline, and returns HTML. |
| Static site generation            | The pipeline runs at build time and writes HTML or other artifacts to disk.                                       |

The rendering mode is selected by the Experience Pack, not by Codex.

Codex only provides the semantic content and APIs.

---

## Presentation Hints

A semantic page may contain presentation hints as part of its content tree.

These hints are preserved by Codex but not interpreted by Codex.

Presentation hints are intended to guide the Experience Pack pipeline without turning Codex into a frontend DSL.

Reasonable presentation hints include:

```json
{
  "layoutIntent": "hero-with-cta",
  "visualPriority": "high",
  "density": "comfortable",
  "navigationRole": "landing-page",
  "breakpointStrategy": "responsive-default",
  "themeTokenSet": "corporate-light"
}
```

Presentation hints should remain:

* declarative,
* stable,
* framework-neutral,
* semantically meaningful,
* readable by agents and pipelines.

Presentation hints should avoid leaking framework-specific implementation details such as:

* CSS selectors,
* React component internals,
* pixel-perfect layout instructions,
* raw class names,
* template engine directives,
* frontend-specific component props.

Codex stores and preserves these hints as content. The Experience Pack interprets them.

---

## Blueprints and User Experience

An Experience Pack may include blueprints that provide a reasonable starting point for users.

Users may:

1. Use the blueprint as-is.
2. Modify the blueprint.
3. Ignore the blueprint and model content types manually.
4. Ask Olorin to materialize the blueprint and generate starter content.

Olorin may select an Experience Pack based on user intent, then use the pack’s blueprints, tools, and instructions to create a working site or experience through Codex public APIs.

Codex does not treat this as a special internal operation. From Codex’s perspective, Olorin is simply creating sites, content types, content items, and revisions.

---

## Security and Trust Model

Experience Packs introduce a future trust boundary.

A pack may include executable pipeline steps, proxy code, renderer logic, or Olorin tools. This means a future implementation must define:

* how Experience Packs are installed,
* whether they are signed or verified,
* what APIs they are allowed to call,
* whether they can execute arbitrary code,
* what permissions they need,
* how secrets are provided to renderers or proxies,
* how logs are redacted,
* how malicious or unsafe packs are rejected,
* how pack-provided Olorin tools are sandboxed.

This ADR does not define the security model. It only records that one will be required before third-party Experience Packs are safe.

---

## Versioning and Evolution

Experience Pack versioning is a major future concern.

A pack may evolve over time:

```text
Corporate Experience Pack v1
  → Page
  → HeroBlock
  → CTA
  → Testimonial

Corporate Experience Pack v2
  → adds backgroundVideo
  → deprecates subtitle
  → changes CTA structure
```

Users may also customize the generated content types:

```text
User modifies Page
  → adds customSEO
  → renames a field
  → removes a block
```

Future work must define how Codex and Olorin handle:

* blueprint versioning,
* compatibility checks,
* blueprint diffs,
* user-customized schemas,
* migration proposals,
* migration execution,
* rollback of failed migrations,
* deprecation of blueprint fields,
* Experience Pack upgrade paths.

Automatic upgrades should not be allowed until compatibility and migration rules are explicit.

---

## Consequences

* Codex remains frontend-agnostic.
* Codex remains the semantic source of truth.
* Rendering and presentation are external responsibilities.
* Experience Packs become a natural distribution unit for the Codex ecosystem.
* A marketplace of Experience Packs becomes possible.
* Olorin can specialize behavior based on pack-provided context.
* The coupling between blueprint and rendering pipeline is explicit and contained.
* Users can adopt a golden path while advanced users can customize pipeline slugs.
* Static, server-rendered, and SPA-oriented outputs can coexist.
* Security and versioning become explicit future concerns.

---

## Non-Goals

This ADR does not define:

* the concrete Experience Pack manifest format,
* the installation protocol,
* the lifecycle of an installed pack,
* a marketplace implementation,
* a sandbox model,
* a renderer/proxy implementation,
* a frontend framework,
* a static-site generator,
* a migration engine,
* a permission model for pack-provided tools.

---

## Open Questions

* What is the exact manifest format of an Experience Pack?
* How are blueprints registered against Codex APIs?
* Should Codex store any reference that an artifact originated from an Experience Pack?
* How should Olorin select an Experience Pack?
* How much authority should Olorin receive from a pack?
* How should Experience Packs be signed or verified?
* Can Experience Pack pipeline slugs execute arbitrary code?
* How are secrets passed to an external renderer/proxy?
* How are blueprint migrations represented?
* How are user-customized content types compared against newer blueprint versions?
* Should presentation hints use a shared vocabulary or be pack-specific?
* What is the compatibility contract between a pack and a Codex version?
* How should caching be coordinated between Codex, the proxy, and static output?

---

## Relationship to Existing ADRs

| ADR     | Relationship                                                                                                                                 |
| ------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| ADR-001 | Experience Packs are external adapters and remain consistent with the adapter-first principle.                                               |
| ADR-002 | Experience Packs consume semantic knowledge units and do not alter the canonical model.                                                      |
| ADR-003 | Experience Pack pipelines are a concrete realization of projection pipelines.                                                                |
| ADR-005 | Experience Packs may consume IndexDocuments through public APIs but do not access the index directly.                                        |
| ADR-006 | Experience Pack proxies are external processes; logging redaction applies to Codex core and should also be considered by external renderers. |
| ADR-008 | Experience Packs consume ContentItem lifecycle outputs but do not change lifecycle semantics.                                                |
| ADR-010 | Experience Packs may trigger audited operations through Codex APIs, but Chronicon remains the audit layer.                                   |
| ADR-011 | Experience Pack renderers and proxies may later expose Observance metrics, but they are outside the current Codex Observance baseline.       |

