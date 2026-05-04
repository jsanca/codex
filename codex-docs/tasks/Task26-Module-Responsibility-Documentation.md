# Task26: Module Responsibility Documentation

## Objective

Create formal documentation describing the responsibility boundaries of the Codex modules.

This task is documentation-only.

Do not create new Maven modules.

Do not move Java classes.

Do not refactor packages.

Do not change runtime wiring.

The goal is to make the architectural map explicit before we start splitting or moving code across modules.

## Decision Context

Codex is evolving from a single core implementation into a modular system.

We already have foundational responsibilities emerging:

- canonical domain model and lifecycle
- indexing and search projections
- durable persistence
- audit/history/timeline
- identity and permissions
- REST/GraphQL/API adapters
- workflow orchestration
- messaging and interaction channels
- synchronization with external editorial surfaces
- AI enrichment
- agent reasoning
- scripting/extensions

Before creating missing modules or moving classes, we need a documented responsibility map.

This prevents accidental dependency cycles, god modules, and premature coupling.

The most important dependency rule:

```text
codex-codex is the canonical core.
codex-codex must not depend on adapter/projection modules such as codex-index, codex-porta, codex-archivum, codex-chronicon, or codex-iter.
```

Modules should depend inward toward the core, not the other way around.

Scope

Implement documentation only.

Create or update one or more documentation files.

Suggested file:

codex-docs/modules/MODULE-RESPONSIBILITIES.md

If the project has a different docs location, use the existing convention.

Do not implement Java code.

Do not create Maven modules.

Do not modify pom.xml.

Do not modify module-info.java.

Do not move classes.

Do not change package names.

Required Content

The document must describe at least these modules:

codex-fundamentum
codex-codex
codex-index
codex-archivum
codex-chronicon
codex-custos
codex-porta
codex-iter
codex-nuntius
codex-speculum
codex-scriptorium
codex-illuminarium
codex-imaginarium
codex-olorin

It should also mention future composition/runtime concerns if needed.

1. Dependency Direction

Add a section named:

Dependency Direction

Explain:

codex-fundamentum
-> used by all modules
-> must stay small and independent

codex-codex
-> depends on fundamentum
-> owns canonical domain model, lifecycle, commands, and domain events
-> must not depend on index, persistence, REST, workflow, audit, AI, or sync modules

adapter/projection modules
-> depend on codex-codex and fundamentum
-> implement infrastructure or projections around the canonical model

Include a dependency sketch:

codex-fundamentum
↑
used by all

codex-codex
-> canonical core
-> depends on fundamentum

codex-index
codex-archivum
codex-chronicon
codex-custos
codex-porta
codex-iter
codex-nuntius
codex-speculum
codex-scriptorium
codex-illuminarium
codex-imaginarium
codex-olorin
-> may depend on codex-codex + fundamentum as needed
-> core must not depend on them

Mention:

Composition/runtime wiring may eventually live in a dedicated runtime/assembly module if wiring multiple modules from outside the core becomes necessary.

Do not create that module in this task.

2. codex-fundamentum

Document:

The Fundamentum is the foundation beneath the manuscript.

Responsibilities:

shared primitives
events
event dispatch abstractions
event subscriber abstractions
transaction context
cache abstractions
concurrency utilities
actor primitives
common exceptions
lifecycle primitives

Rules:

must remain small
must not depend on CMS/domain concepts
must not depend on codex-codex
must not contain content-specific logic
should provide stable, reusable building blocks
3. codex-codex

Document:

Codex core is the canonical manuscript model.

Responsibilities:

canonical domain model
Site
ContentType
ContentTypeVersion
ContentItem
ContentRevision
field definitions
commands
lifecycle services
domain events
domain validation
semantic exceptions
in-memory MVP repositories if currently located there
event publishing decorators around canonical services

Rules:

owns the truth of the CMS model
owns lifecycle semantics
emits domain events
must not perform indexing directly
must not perform search directly
must not persist to specific durable backends directly
must not expose REST/GraphQL
must not own workflow orchestration
must not own full identity/permission logic
should only know minimal Actor information from fundamentum
4. codex-index

Document:

The Index makes knowledge discoverable.

Responsibilities:

indexing abstractions
IndexDocument
IndexDocumentId
IndexResourceType
IndexWriter
search services
query abstractions
indexing projection subscribers
mapping domain events into index documents
ranking and retrieval strategies
future adapters for:
Lucene
OpenSearch
Elasticsearch
Codex/myIR
vector indexes
hybrid search systems

Rules:

does not own canonical content
receives projections derived from domain events
may depend on codex-codex and codex-fundamentum
codex-codex must not depend on codex-index
canonical services must not call indexing directly
public content indexing should usually be driven by published content events

Mention current/future boundary:

Indexing foundation may currently exist inside codex-codex during MVP.
Future tasks may move indexing abstractions and subscribers into codex-index after module boundaries are finalized.
5. codex-archivum

Document:

The Archivum stores the manuscripts.

Responsibilities:

durable repository implementations
relational database adapters
filesystem-backed storage
object/blob storage
S3/MinIO-style storage
transaction adapters
migration support
outbox storage in the future
storage of canonical structures, binary assets, generated artifacts, or large objects depending on backend

Rules:

implements persistence
does not own domain lifecycle
does not own search
does not own cache
does not own audit
does not own workflow
does not own rendering
does not own AI enrichment
depends inward on core contracts
6. codex-chronicon

Document:

The Chronicon preserves the memory of the manuscript.

Responsibilities:

audit trails
event history
historical timelines
change narratives
temporal projections
restoration views
explaining how resources changed over time

Rules:

listens to domain events
records what happened
does not own canonical lifecycle
does not replace domain events
does not perform indexing/search
does not perform workflow orchestration

Clarify:

Audit is a product/domain history concern.
Observability is an operations concern and should remain separate.
7. codex-custos

Document:

The Custos guards the manuscript.

Responsibilities:

identity
users
groups
teams
roles
permissions
access policies
authorization decisions
future integration with external identity providers

Rules:

codex-codex should know only minimal Actor information
full user/role/permission logic belongs in Custos
canonical services may eventually consult authorization boundaries through explicit ports
do not leak external auth frameworks into core
8. codex-porta

Document:

The Porta is the gate.

Responsibilities:

REST adapters
GraphQL adapters
WebSocket endpoints
SSE endpoints
API controllers
request/response mapping
authentication boundary integration
external API exposure

Rules:

depends on core/services
core does not depend on Porta
does not own domain behavior
does not own persistence
maps external protocols to commands/services
should not bypass canonical services

Mention:

Porta may expose human, system, or agent-facing APIs.
9. codex-iter

Document:

The Iter is the path through the manuscript.

Responsibilities:

workflow orchestration
process state
workflow steps
conditional branching
waiting states
background task orchestration
continuation after events
actions triggered by workflow transitions

Rules:

listens to domain events
may execute commands through canonical services
may enter WAITING_FOR_INPUT
should not own canonical content state
should not bypass lifecycle services
should not become the audit system

Mention:

Iter can orchestrate, but Codex core remains the source of truth for lifecycle state.
10. codex-nuntius

Document:

The Nuntius carries messages.

Responsibilities:

human interaction messages
agent interaction messages
prompts and replies
approval requests
notifications
conversational state around operations
interaction channels between users, agents, and workflow

Rules:

transports interaction
does not own canonical content
does not own workflow orchestration
does not own AI reasoning
may support Iter when workflows wait for human/agent input

Mention relationship:

Nuntius transports interaction.
Iter orchestrates process.
Olórin reasons and proposes actions.
Porta exposes external channels.
11. codex-speculum

Document:

The Speculum is the mirror.

Responsibilities:

synchronization with external editorial/storage surfaces
projections between Codex and:
Google Drive
Git repositories
local filesystem folders
external document systems
static site projections
controlled ingestion of external changes
controlled reflection of Codex state outward
future sync conflict handling
future idempotent adapter commands

Rules:

does not replace Codex as canonical model
external systems are reflections or controlled inputs
must use canonical commands/services when ingesting changes
belongs to adapter/sync/orchestration territory
should not weaken domain invariants

Mention:

Speculum may eventually require strong idempotency semantics for commands invoked by external adapters.
12. codex-scriptorium

Document:

The Scriptorium is where custom rules are written.

Responsibilities:

scripting extensions
extension points
custom business rules
controlled automation
future lifecycle hooks
possibly JavaScript or other sandboxed extension execution

Rules:

extensions must not compromise the canonical model
core should prefer explicit services and event subscribers
scripting is an extension layer, not the foundation of domain behavior
must be controlled and sandboxed if dynamic execution is introduced
13. codex-illuminarium

Document:

The Illuminarium enriches the manuscript.

Responsibilities:

enrichment pipelines
embeddings
classification
metadata extraction
content analysis
summarization support
semantic enrichment
AI-assisted annotations

Rules:

does not own canonical content
may write enrichment projections or metadata through controlled services
may feed index/search/RAG projections
should not perform lifecycle operations directly without commands/services
14. codex-imaginarium

Document:

The Imaginarium is the AI integration space.

Responsibilities:

AI provider connectors
model integration
tool orchestration infrastructure
prompt/runtime abstractions
agent runtime concepts
LLM-based capabilities shared by higher-level agents

Rules:

provides AI infrastructure
does not own Codex domain lifecycle
should not bypass canonical services
may support Olórin and Illuminarium
15. codex-olorin

Document:

Olórin is the guide.

Responsibilities:

guided Codex-aware agent
reasoning over Codex capabilities
proposing actions
asking for approval
executing tools through controlled interfaces
helping users create sites, content types, content items, workflows, and other resources

Rules:

uses tools/services
does not own canonical state
must respect permissions and workflow gates
should ask for approval where required
may coordinate with Nuntius, Iter, Imaginarium, and Porta
16. Cross-Module Responsibility Matrix

Add a matrix.

Suggested columns:

Module | Owns Canonical State? | Listens to Events? | Exposes APIs? | Uses AI? | Provides Infrastructure?

Example rows:

fundamentum | No | Provides primitives | No | No | Yes
codex-codex | Yes | Emits events | Internal services | No | Minimal
index | No | Yes | Search APIs later | Possibly | Yes
archivum | No | Maybe | No | No | Yes
chronicon | No | Yes | History APIs later | No | Yes
custos | Identity state | Maybe | Auth APIs later | No | Yes
porta | No | No/Maybe | Yes | No | Yes
iter | Workflow state | Yes | Workflow APIs later | No | Yes
nuntius | Interaction state | Maybe | Messaging APIs later | Maybe | Yes
speculum | Sync state | Yes/Maybe | Adapter APIs | Maybe | Yes
scriptorium | Extension state | Maybe | Extension APIs | Maybe | Yes
illuminarium | Enrichment projections | Yes/Maybe | Enrichment APIs later | Yes | Yes
imaginarium | AI runtime state | Maybe | AI APIs later | Yes | Yes
olorin | Agent session state | Yes/Maybe | Agent interaction APIs | Yes | Yes

Keep the matrix readable.

Do not over-perfect it.

17. Active / Near-Future / Future-Forward Classification

Add a section:

Implementation Classification

Classify:

Active / Current
canonical core
event pipeline
content type/item/revision lifecycle
indexing foundation
cache foundation
local event subscribers
Near-Future
moving indexing classes to codex-index
ContentSearchService
cache decorators
cache invalidation subscribers
Caffeine adapter
module skeleton creation
runtime/assembly module discussion
Future-Forward
dynamic subscriber registry
tenant-aware index ids
PublishedPointer as separate entity
full _system site / multi-tenant registry
Speculum sync
Scriptorium scripting
Iter workflow engine
Nuntius messaging
Illuminarium enrichment pipelines
Imaginarium AI runtime
Olórin agent
Concordia cluster coordination

Mention:

Future-forward concepts should be documented and kept in mind, but not implemented unless a task explicitly says so.
18. Open Questions

Add a section:

Open Questions

Include:

Should runtime composition eventually move to a dedicated codex-runtime or codex-assembly module?
When should indexing abstractions move from the current MVP location to codex-index?
Should IndexDocumentIdFactory wait until tenant-aware indexing is required?
Where should PublishedPointer live if it becomes a real entity?
Which module owns cache invalidation subscribers: core, cache infrastructure, or specific projection modules?
How will Custos integrate with core without leaking full user/permission models into codex-codex?
Which module will own Concordia if cluster coordination becomes necessary?

Do not resolve these questions in code.

19. Post-Task Report

After implementation, report:

files created or updated
whether any existing documentation was modified
any intentional deviations
any open questions discovered
recommended follow-up tasks
20. Constraints
    Documentation-only task.
    Do not write Java code.
    Do not create Maven modules.
    Do not move classes.
    Do not refactor packages.
    Do not modify pom.xml.
    Do not modify module-info.java.
    Do not modify runtime wiring.
    Do not modify tests.
    Do not modify unrelated files.
    Keep documentation in English.
    Keep module responsibilities clear and concise.
    Prefer architectural clarity over excessive detail.