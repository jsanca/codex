# Task CODEX-KNOW-002 — Codex OSK Artifact Discipline and Engineering Log

Status: Planned
Owner: Elito
Role: Knowledge Curator
Target: 20–30 minutes
Hard Stop: 45 minutes

## Execution Requirements

Apply, if available:

* `.opencode/skills/osk-architecture-review/SKILL.md`
* `.opencode/skills/osk-engineering-reporting/SKILL.md`
* `.opencode/skills/osk-knowledge-curation/SKILL.md`

No commits.

This is documentation and structure work only.

## Objective

Refine Codex’s OSK artifact structure so tasks, reports, reviews, checkpoints, and engineering logs become recoverable, traceable, and organized by responsibility/module.

Codex already has a minimum OSK structure. This task improves it without overbuilding.

## Context

Previous Elito task created:

* `docs/agents/templates/implementation-task-template.md`
* `docs/agents/templates/implementation-report-template.md`
* `docs/agents/templates/review-report-template.md`
* `docs/agents/templates/recovery-checkpoint-template.md`
* `docs/agents/tasks/Task-CODEX-016—SecuredRuntimeComposition.md`
* `docs/agents/reports/`
* `docs/agents/reviews/`
* `docs/agents/checkpoints/`
* `docs/agents/templates/`

Existing historical structure includes:

* `docs/agents/tasks/custos/`
* `docs/agents/tasks/knowledge/`
* `docs/agents/tasks/reviewer/`
* `docs/agents/tasks/transversal/`
* `docs/agents/reviewer/` singular historical directory

Jonathan prefers organizing tasks by responsibility/module, not only as a flat task list.

Current preferred responsibility buckets:

* `custos`
* `runtime`
* `knowledge`
* `reviewer`
* `transversal`

## Scope

### In Scope

1. Create or verify responsibility-based task directories:

```text
docs/agents/tasks/
  custos/
  runtime/
  knowledge/
  reviewer/
  transversal/
```

2. Create matching report directories if useful:

```text
docs/agents/reports/
  custos/
  runtime/
  knowledge/
  reviewer/
  transversal/
```

3. Create matching review directories if useful:

```text
docs/agents/reviews/
  custos/
  runtime/
  transversal/
```

Do not force unnecessary directories if there is no clear current use.

4. Move or recreate `Task-CODEX-016—SecuredRuntimeComposition.md` under:

```text
docs/agents/tasks/runtime/Task-CODEX-016—SecuredRuntimeComposition.md
```

Only move it if safe. If moving creates risk or duplicate confusion, leave the original and create a short index/reference note.

5. Create:

```text
docs/engineering/ENGINEERING_LOG.md
```

If `docs/engineering/` does not exist, create it.

6. Add initial engineering log entries for:

* Codex OSK alignment
* Custos Phase 1.4 secured decorators completion
* Decision not to use `Forwarding*Service` for secured decorators
* Phase 3.1 Secured Runtime Composition selected as next runtime work
* Runtime metadata idea: secured vs unsecured runtime

7. Update templates if needed so future tasks include:

* Task ID
* Status
* Owner
* Role
* Target time
* Hard stop
* Execution requirements / skills
* No commits
* Checkpoint requirement
* Engineering report requirement
* Validation commands

8. Add a short note explaining artifact meanings:

```text
Task       = what should be done
Report     = what was done
Review     = what was audited
Checkpoint = how to recover context
Engineering Log = human-readable timeline
Roadmap    = direction and phases
ADR        = durable architectural decision
```

This note may live in:

```text
docs/agents/README.md
```

or another appropriate file if one already exists.

## Out of Scope

Do not modify:

* production code
* tests
* module-info.java
* Maven files
* Custos behavior
* Concilium runtime behavior
* CODEX-016 technical scope

Do not create a large root `knowledge/` tree yet unless the existing project already clearly expects it.

Do not delete historical directories such as:

```text
docs/agents/reviewer/
```

Instead, mark them as historical or leave them untouched.

## Acceptance Criteria

* Responsibility-based task structure exists or is verified.
* CODEX-016 lives under the runtime task responsibility area, or a clear reference explains why it remains where it is.
* `docs/engineering/ENGINEERING_LOG.md` exists.
* Engineering log contains initial dated entries.
* Artifact meanings are documented.
* Templates support OSK-style timebox/report/checkpoint discipline.
* Existing historical files are not destroyed.
* `git diff --check` passes.
* No tests are required because this is docs-only.

## Deliverables

* Updated/created directory structure
* `docs/engineering/ENGINEERING_LOG.md`
* Optional `docs/agents/README.md`
* Updated templates if needed
* Relocated or referenced CODEX-016 task file
* Short completion report

## Architectural Notes

Codex should adopt OSK discipline without copying Arbitrieri 1:1.

Arbitrieri has a mature artifact structure, but Codex should keep a smaller structure focused on current needs.

The goal is recoverability and traceability, not bureaucracy.

Codex product architecture remains independent of OSK runtime concepts.

OSK governs the work process:

```text
Clio builds.
Deep audits.
Elito explains.
Brio researches.
Elo keeps the thread.
Jonathan decides.
```

## Definition of Done

* Codex has a clear place for future runtime, Custos, knowledge, reviewer, and transversal tasks.
* Codex has an engineering log.
* CODEX-016 is ready to be handed to Clio as a formal task.
* No stale or conflicting structure is introduced.
* Validation completed.
* No commits performed.
