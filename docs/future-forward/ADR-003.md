ADR: Projections as transformation pipelines

Decision:
Codex will treat projections as explicit transformation pipelines. A projection receives canonical knowledge units and produces a target-specific output such as static HTML, API payloads, PDF, email, mobile cards, search documents, or agent descriptors.

Rationale:
Codex should not store presentation as its canonical truth. The same knowledge unit may need to be consumed by humans, web applications, mobile applications, static sites, search engines, and AI agents. A pipeline model allows Codex to enrich, validate, filter, transform, and render content without coupling the core model to a specific interface.

Consequences:
- HTML is a render output, not the source of truth.
- Static sites can be generated from public site structure, routes, published knowledge units, assets, metadata, and renderer configuration.
- Agent descriptors are also projections.
- Search indexes are also projections.
- Projection pipelines can be composed from domain capabilities/tools.