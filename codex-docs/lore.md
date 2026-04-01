# The Lore of Codex

Codex is inspired by the ancient manuscripts that once preserved the knowledge of civilizations.
Before printing presses and digital systems, knowledge lived in carefully crafted books known as *codices*. These books were not merely collections of pages; they were structured repositories of thought, culture, and discovery.

A codex was written, copied, illuminated, archived, and indexed by many hands. Each role contributed to preserving and expanding the body of knowledge.

The Codex platform borrows this metaphor to describe the architecture of the system.

Instead of treating a content platform as a monolithic application, Codex views the system as a living manuscript composed of specialized roles and components.

Every module in Codex reflects a function that once existed in the world of manuscripts.

---

# The World of Codex

The Codex ecosystem is composed of several conceptual components inspired by the historical lifecycle of manuscripts.

## Codex

The **Codex** is the system as a whole.

It represents the living book that stores structured knowledge. Its purpose is to manage content types, content items, and their lifecycle while remaining minimal and extensible.

The Codex core is intentionally small. It defines the language and structure of the system but delegates most responsibilities to specialized modules.

---

## Chronicle

The **Chronicle** preserves history.

Every manuscript evolves over time. Changes are recorded so that knowledge can be traced, restored, and understood in context.

Within Codex, the Chronicle manages:

- versioning
- publishing lifecycle
- historical records of content

It ensures that nothing is lost and that every change becomes part of the system's narrative.

---

## Archive

The **Archive** stores the manuscripts.

Historically, archives preserved texts so they could survive generations. In Codex, the Archive represents the storage layer.

The Archive is designed to be replaceable and pluggable.

Possible archive implementations include:

- filesystem storage
- relational databases
- object storage systems

By separating storage from the core, Codex allows infrastructures to evolve without altering the knowledge model.

---

## Index

The **Index** makes knowledge discoverable.

Large manuscripts required indexes so readers could navigate them efficiently. In Codex, the Index provides search and retrieval capabilities.

The Index may be implemented using different search technologies such as:

- Lucene
- OpenSearch
- other search engines

Like the Archive, the Index is implemented through plugins.

---

## Scriptorium

The **Scriptorium** is the place where manuscripts are written and transformed.

In medieval monasteries, scribes worked in scriptoria copying and modifying texts. In Codex, the Scriptorium represents the runtime environment where dynamic behavior can be executed.

The Scriptorium enables:

- scripting extensions
- lifecycle hooks
- business logic customization

This allows organizations to adapt Codex without modifying its core.

---

## Illuminator

The **Illuminator** enriches manuscripts.

Medieval manuscripts were often decorated and enhanced by illuminators who added visual and symbolic layers to the text.

In Codex, the Illuminator represents systems that enrich content through automated processes.

Examples include:

- AI enrichment
- embeddings generation
- classification
- metadata extraction

The Illuminator transforms content into something more meaningful and searchable.

---

## Gateway

The **Gateway** exposes the Codex to the outside world.

Just as manuscripts were eventually read and shared, Codex exposes its knowledge through APIs and services.

The Gateway is responsible for:

- REST APIs
- GraphQL APIs
- external integrations

It acts as the bridge between the internal world of Codex and external applications.

---

# A Living Manuscript

Codex is not designed as a static system.

It is intended to grow gradually as new components, plugins, and extensions are added.

Every plugin becomes another contributor to the manuscript.
Every extension becomes another layer of interpretation.
Every deployment becomes another edition of the book.

Just like the codices of history, the Codex platform is meant to evolve — page by page, chapter by chapter.
