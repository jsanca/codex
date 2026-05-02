ADR: Treat external workspaces as adapters, not as the canonical content model

Decision:
Codex will model Google Drive, local folders, and similar systems as mirror adapters.
These adapters expose human-friendly projections of Codex entities, but they do not define the domain model.

Rationale:
Users already create and collaborate in tools such as Google Docs.
Competing with mature editors is not the first goal of Codex.
Instead, Codex should integrate with those tools while preserving its own semantic model:
sites, content types, content items, revisions, assets, permissions, events, and publishing state.

Consequences:
- Codex remains storage-agnostic.
- Google Drive support can be replaced or complemented by Dropbox, OneDrive, Git, or local filesystem adapters.
- Sync conflicts become an explicit domain concern.
- Adapters must translate external changes into Codex commands rather than mutating the core directly.