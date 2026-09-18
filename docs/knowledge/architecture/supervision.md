# Supervision Model

This note documents the conceptual supervision model for the Imaginarium Agent
Runtime.

It is documentation only. It does not define implementation classes, Java APIs, or
runtime hooks.

## Purpose

Supervision exists to observe AI runtime behavior and surface meaningful signals.

It helps future applications notice loops, drift, policy-relevant events, or
mission-quality issues without making the supervisor a hidden business actor.

## Core Distinctions

### Orchestrator

The orchestrator coordinates high-level flow.

It may decide what conceptual component or process should be invoked next, but it
does not automatically own domain authority.

### Planner

The planner turns a mission into proposed steps, options, or structured intent.

Planning is proposal work. It does not approve privileged actions and does not
replace policy or authorization checks.

### Runtime

The runtime provides the execution environment.

It manages lifecycle, sessions, contexts, registries, model/provider abstraction,
planning infrastructure, and supervision infrastructure.

### Supervisor

The supervisor observes runtime behavior.

Supervisors are observers, not business actors. They emit events or signals. They
do not directly modify execution, silently cancel domain operations, or mutate
canonical state.

## Decision: Observation Rather Than Direct Intervention

Supervision uses observation rather than direct intervention.

Default behavior should conceptually be equivalent to logging or no-op. This keeps
the runtime predictable and avoids hiding business behavior inside supervision.

Applications may later decide how to react to supervision events, but that reaction
belongs outside the supervisor concept itself.

## Conceptual Daemon Responsibilities

The following names describe conceptual daemon responsibilities. They are not
implementation class names.

### Loop Guard

Observes possible repetitive execution patterns.

May emit a signal that a mission or session appears to be looping.

Does not directly terminate execution.

### Execution Observer

Observes runtime activity and records notable execution signals.

May emit events useful for logs, metrics, audit-adjacent review, or developer
inspection depending on future application policy.

Does not decide domain authorization.

### Mission Evaluator

Observes whether execution appears aligned with the mission.

May emit signals about progress, drift, ambiguity, or completion confidence.

Does not redefine the mission by itself.

## Boundary

Supervision does not:

* execute business operations
* approve privileged actions
* replace Custos
* mutate canonical content
* silently alter runtime execution
* define persistence or transport behavior

It creates observable signals so applications can decide how to react explicitly.
