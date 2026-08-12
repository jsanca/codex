# Codex OSK Operating Model Alignment

This document explains how Codex uses the OSK-style operating model to coordinate
work, reviews, documentation, research, and architectural memory.

It is an operations document. It does not couple Codex product architecture to OSK
runtime internals, and it does not define product modules, Java APIs, or
implementation plans.

## Codex Product vs Codex As OSK Proving Ground

Codex is a product with its own domain-driven and module-driven architecture.

Its product architecture is organized around Codex modules such as `codex-codex`,
`codex-custos`, `codex-chronicon`, `codex-index`, `codex-concilium`,
`codex-imaginarium`, and future modules.

The OSK operating model governs how Codex work is coordinated:

* how tasks are sliced
* how implementation and review are separated
* how architecture decisions are captured
* how documentation memory is maintained
* how research informs, but does not automatically decide, direction

Codex core modules must not depend on OSK runtime concepts. OSK is a working model
for building Codex, not a hidden product dependency.

Codex may still serve as a proving ground for OSK-style collaboration. That means
the project can refine how agents coordinate, review, document, and checkpoint
work, while keeping Codex product architecture clean.

## Agent Responsibilities

The current working responsibilities are:

```text
Clio builds.
Deep audits.
Elito explains.
Brio researches.
Elo keeps the thread.
Jonathan decides.
```

### Clio

Clio implements production code and tests.

Clio carries tasks through code changes, verification, and concise implementation
reports.

### Deep

Deep reviews architecture, behavior preservation, risk, and security-sensitive
semantics.

Deep does not write production code during review. Deep identifies blockers,
warnings, follow-ups, and alignment gaps.

### Elito

Elito maintains documentation, diagrams, ADRs, conceptual foundations, and
architectural memory.

Elito turns completed movement and accepted direction into durable written
knowledge.

### Brio

Brio performs research.

Research informs direction but remains advisory until Jonathan accepts it into the
project's architecture or task stream.

### Elo

Elo coordinates direction, task slicing, sequencing, reviews, and architectural
judgment.

Elo keeps the thread coherent across agents, phases, and context boundaries.

### Jonathan

Jonathan owns final decisions.

Architectural decisions, major direction changes, and accepted trade-offs require
Jonathan's approval.

## Work Artifact Vocabulary

### Phase

A phase is a broad area of project progress with a goal and success shape.

Phases should be stable enough to guide work, but not so detailed that they become
task logs.

### Task / Slice

A task or slice is a small, reviewable unit of work.

It should have a clear expected output and a bounded scope. Implementation slices
should be small enough for Deep to review meaningfully.

### Checkpoint

A checkpoint is a durable summary after meaningful progress.

It captures what changed, what was decided, what remains open, and where future
agents should resume.

### Review

A review is an explicit assessment of behavior, architecture, risk, security, or
documentation alignment.

Review should be separate from implementation so that findings are easier to trust.

### Report

A report is a short artifact produced after implementation, review, research, or
documentation work.

Reports preserve local task memory without turning the roadmap into a noisy log.

### ADR

An ADR captures an architectural decision.

ADRs should be used for decisions that affect boundaries, semantics, invariants,
or future design direction.

### Skill

A skill is an agent guidance artifact for a repeatable kind of work.

Skills guide agents but do not replace human architectural judgment.

### Roadmap Update

A roadmap update changes the living project map.

Roadmap updates should happen after meaningful phase movement, major decisions,
or discovered gaps. They should not happen for every tiny task.

## Workflow Rules

Codex uses the following workflow discipline:

* Work should be divided into small reviewed slices.
* Implementation and review are separate responsibilities.
* Documentation follows completed architectural movement or explicitly accepted
  conceptual direction.
* The roadmap is living but not noisy.
* Research is advisory until accepted.
* Architectural decisions require Jonathan approval.
* Product modules remain governed by Codex architecture, not OSK internals.
* Follow-up tasks should be reported rather than implemented opportunistically.

This discipline protects both speed and architectural memory.

## Checkpoint Discipline

Create a checkpoint when the project reaches a durable boundary.

Recommended checkpoint moments:

* after completing a phase
* after completing a major secured decorator or subsystem
* after an ADR-worthy decision
* before switching projects for a while
* after Deep finds a significant gap
* after a sequence of related tasks changes the project model

Checkpoints should capture:

* completed work
* current behavior
* accepted decisions
* known limitations
* open questions
* recommended next tasks

Checkpoint documents should be concise enough to read quickly but complete enough
to restart work safely.

## Relationship To OSK Skills

Codex may use OSK skills for:

* boundary review
* roadmap synchronization
* documentation review
* architecture review
* security-sensitive review
* implementation hygiene
* research synthesis

Skills guide agents through repeatable procedures. They do not replace Jonathan's
architectural judgment and do not automatically become Codex product architecture.

When a skill materially shapes a task, reports should mention it so future agents
can understand how the work was performed.

## Immediate Recommendation

The current Custos Phase 1.4 secured decorator work should produce a checkpoint:

```text
CHECKPOINT-CODEX-CUSTOS-SECURED-DECORATORS.md
```

That checkpoint should summarize:

* AccessDecisionService wiring status
* domain permission service status
* ContentType archive vocabulary alignment
* SecuredContentItemService status
* SecuredContentTypeService status
* SecuredSiteService status
* fail-closed delete/restore/unarchive semantics
* pass-through list/read filtering gaps
* authorization matrix coverage
* remaining runtime composition boundary

This is a recommendation, not an implementation requirement for this document.

## Boundary

This operating model does not mean:

* Codex product modules depend on OSK.
* OSK skills decide architecture.
* Research is accepted without review.
* Documentation replaces implementation verification.
* Checkpoints replace ADRs.

It means Codex has an explicit collaboration model for preserving progress,
quality, and architectural memory.
