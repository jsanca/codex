# ADR-004: Supervision Uses Observation Rather Than Direct Intervention

## Status

Accepted as conceptual foundation.

## Context

AI runtime supervision is useful for noticing loops, drift, execution quality, or
policy-relevant signals.

However, if supervisors directly modify execution or domain state, supervision can
become hidden business behavior.

## Decision

Supervision uses observation rather than direct intervention.

Supervisors are observers, not business actors. They emit events or signals. They
do not directly mutate execution, silently cancel domain operations, approve
privileged actions, or change canonical state.

Default supervision behavior should be conceptually equivalent to logging or no-op.
Applications may later decide how to react to supervision events.

## Consequences

Supervision remains explainable and externally governable.

Runtime behavior does not change invisibly because a supervisor observed something.
Any future reaction to supervision signals must be explicit in the consuming
application.

## Boundaries

This ADR does not define event classes, hooks, callback APIs, or policy engines.
