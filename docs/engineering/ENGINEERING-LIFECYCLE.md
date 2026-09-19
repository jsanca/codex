# Codex Engineering Lifecycle

Status: Current Codex project policy  
Scope: Non-trivial engineering objectives

## Purpose

OSK supplies reusable engineering concepts, artifact discipline, and agent
capabilities. It does not prescribe one universal artifact sequence for every
project. Codex adopts the lifecycle in this document as a stricter project
policy for non-trivial engineering objectives.

The policy makes authorization to advance visible in repository artifacts rather
than relying on conversational memory.

## Lifecycle

```text
Objective
  -> Intent
  -> Use Cases
  -> Canonical Test Cases
  -> Engineering Plan
  -> Pre-Implementation Review
  -> Reconciliation, if required
  -> Design
  -> Design Review
  -> Reconciliation, if required
  -> Implementation Slices
  -> Checkpoint Reviews defined by the plan
  -> Final Engineering Review
  -> QA / Knowledge Reconciliation
  -> Release / Deploy / Objective Closeout
```

Not every objective needs every artifact at the same depth. The lifecycle is a
control for non-trivial work, not a claim that OSK requires this exact sequence
everywhere.

Reviews are conditional feedback loops, not a mandatory fixed `fix` stage:

```text
Review
  PASS
    -> advance

  PASS WITH MINOR CHANGES
    -> reconcile
    -> re-review when necessary

  CHANGES REQUIRED
    -> reconcile
    -> re-review
```

Reconciliation corrects the affected artifact or evidence. It does not silently
override behavior, design, or review authority.

## Authority Model

```text
Behavior authority:
  Intent
    -> Use Cases
      -> Canonical Test Cases

Execution authority:
  Engineering Plan

Technical authority:
  Approved Design

Progress authority:
  Review Gates
```

The Engineering Plan is the execution source of truth. It remains subordinate
to approved Intent, Use Cases, Canonical Test Cases, Design, and review gates.
An implementation agent must not execute a valid-looking plan while a required
gate remains pending.

## Gates

### Gate P - Planning Approved

Expected inputs:

- Intent
- Use Cases
- Canonical Test Cases
- Engineering Plan
- Pre-Implementation Review

The objective may proceed to Design when blocking planning findings are resolved
and the review permits advancement.

### Gate D - Design Approved

Expected inputs:

- approved planning package
- Design
- Design Review

Implementation must not begin before Gate D is satisfied.

### Gate I - Implementation Complete

Expected evidence:

- planned implementation slices are complete
- required intermediate checkpoints are complete
- production and test changes conform to the approved design
- implementation tests are green

### Gate Q - Objective Complete

Expected evidence:

- Final Engineering Review
- QA / Knowledge Reconciliation
- synchronized documentation
- engineering-log and roadmap state updated where applicable
- release, deploy, or objective-closeout requirements satisfied

## Engineering Plan Requirement

Every non-trivial engineering plan should contain a visible lifecycle section:

```text
## Lifecycle / Execution Gates

Current stage: <stage>

Implementation authorized: YES / NO

Pending gate:
  <gate or none>

Approved behavioral sources:
  - Intent
  - Use Cases
  - Test Cases

Approved technical source:
  - Design, once Gate D passes

Execution sequence:
  1. ...
  2. ...
  3. ...

Required review checkpoints:
  - ...
```

This section is an execution control. It does not replace the source artifacts,
their acceptance criteria, or the review record.

## Artifact Placement

Use the existing OSK structure when recording this lifecycle:

- Intent and review tasks: `docs/engineering/agents/tasks/` and
  `docs/engineering/agents/reviews/`
- Use Cases and Canonical Test Cases: `docs/knowledge/`
- Engineering plans and designs: `docs/engineering/plan/` and
  `docs/engineering/design/`
- Reports and checkpoints: `docs/engineering/agents/reports/` and
  `docs/engineering/agents/checkpoints/`

The concrete paths may vary by objective. The authority and gate relationships
do not.
