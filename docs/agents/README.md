# Codex Agent Artifacts

Codex uses a small OSK-aligned artifact structure to keep work recoverable and
traceable without copying larger projects 1:1.

## Directory Structure

```text
docs/agents/
  tasks/
    custos/
    runtime/
    knowledge/
    reviewer/
    transversal/
  reports/
    custos/
    runtime/
    knowledge/
    reviewer/
    transversal/
  reviews/
    custos/
    runtime/
    transversal/
  checkpoints/
  templates/
```

The historical `docs/agents/reviewer/` directory remains untouched. New formal
review artifacts should use `docs/agents/reviews/`.

## Artifact Meanings

Task = what should be done.

Report = what was done.

Review = what was audited.

Checkpoint = how to recover context.

Engineering Log = human-readable timeline.

Roadmap = direction and phases.

ADR = durable architectural decision.

## Responsibility Buckets

Use responsibility/module buckets when a task naturally belongs to one:

* `custos`
* `runtime`
* `knowledge`
* `reviewer`
* `transversal`

Prefer the smallest useful bucket. Do not create new responsibility folders unless
the project has a clear repeated need.

## Discipline

Tasks define intent and boundaries before work starts.

Reports summarize what actually changed.

Reviews remain separate from implementation.

Checkpoints preserve recovery state after significant movement.

The engineering log records the human-readable timeline without turning the
roadmap into a noisy task ledger.
