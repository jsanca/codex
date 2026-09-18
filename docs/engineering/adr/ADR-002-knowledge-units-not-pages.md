ADR: Codex manages knowledge units, not pages

Decision:
Codex will model content as structured knowledge units rather than web pages.
A web page is treated as one possible projection of content, not the canonical form.

Rationale:
Modern content is consumed by websites, mobile apps, desktop apps, APIs, emails, search engines, and AI agents.
Using HTML or page-oriented models as the canonical representation couples the system to a specific interface.
Codex should preserve semantic intent, structure, identity, versioning, relationships, and lifecycle independently of presentation.

Consequences:
- Rich text fields should use a structured semantic document model, not raw HTML as the primary representation.
- Renderers may produce HTML, React components, mobile views, Markdown, PDF, email HTML, or agent context.
- Google Docs and similar tools are editorial surfaces, not canonical content stores.
- Content types should represent knowledge structures, not only pages.
- Publishing becomes projection-specific.