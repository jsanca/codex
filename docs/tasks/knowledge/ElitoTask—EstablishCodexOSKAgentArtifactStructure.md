Elito Task — Establish Codex OSK agent artifact structure

role: Knowledge Curator

Context:
Codex has adopted the OSK operating model, but its task/report/review/checkpoint discipline is not yet as formal as Arbitrieri.

Arbitrieri uses a structured documentation layout with:

* docs/agents/tasks
* docs/agents/reports
* docs/agents/reviews
* docs/agents/checkpoints
* docs/agents/templates
* docs/adr
* docs/roadmap
* knowledge/

Codex already has some checkpoints and roadmap docs, but upcoming Phase 3.1 Secured Runtime Composition should be executed as a formal OSK task with task, report, review, and checkpoint artifacts.

Goal:
Create the minimum OSK-aligned agent artifact structure for Codex without overbuilding.

Scope:
Documentation and directory/template setup only.

Create or verify:

docs/agents/
├── tasks/
├── reports/
├── reviews/
├── checkpoints/
└── templates/

If directories already exist, do not duplicate them.

Add templates:

1. docs/agents/templates/implementation-task-template.md

Should include:

* Task ID
* Title
* Status
* Owner
* Role
* Target time
* Hard stop
* Execution requirements
* Objective
* Context
* Scope
* Out of scope
* Acceptance criteria
* Deliverables
* Architectural notes
* Definition of done
* Validation
* Checkpoint requirement

2. docs/agents/templates/implementation-report-template.md

Should include:

* Task ID
* Files changed
* Production changes
* Test changes
* Validation commands
* Test results
* Decisions made
* Deviations from task
* Follow-ups
* Checkpoint created or updated

3. docs/agents/templates/review-report-template.md

Should include:

* Review ID
* Reviewed task/report
* PASS/WARN/BLOCKER summary
* Files inspected
* Boundary assessment
* Behavior assessment
* Test assessment
* Risks
* Required fixes
* Recommendation

4. docs/agents/templates/recovery-checkpoint-template.md

Should include:

* Checkpoint ID
* Date
* Completed work
* Current state
* Decisions
* Accepted gaps
* Risks
* Next recommended task
* Recovery instructions

Also create the first formal task file:

docs/agents/tasks/Task-CODEX-016—SecuredRuntimeComposition.md

The task should formalize Phase 3.1 Secured Runtime Composition using the approved Option B:

* Custos exposes public SecuredServiceComposer in codex.custos.api.service.
* Secured*Service implementations remain internal.
* Concilium depends on Custos.
* codex-codex remains pure.
* ConciliumRuntime.secured(Supplier<PermissionResolutionSnapshot>) exposes secured services.
* ConciliumRuntime.inMemory() remains explicit unsecured/back-compat path.
* Runtime metadata should indicate whether the runtime is SECURED or UNSECURED.

Execution requirements:

* Apply .opencode/skills/osk-architecture-review/SKILL.md if available.
* Apply OSK engineering reporting skill if available.
* No commits.
* Create or update checkpoint after implementation.

Out of scope for CODEX-016:

* REST/Porta
* Archivum/persistence
* Olorin
* workflow
* collection read filtering
* alias-to-SiteKey authorization
* restore/purge/unarchive semantics
* changing Custos authorization semantics
* renaming ConciliumRuntime.inMemory()

Validation:

* git diff --check

Expected output:

* directories created/verified
* templates added
* CODEX-016 task file created
* any existing conflicting structure reported
