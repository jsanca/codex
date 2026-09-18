# Conceptual Runtime Model

This document explains the conceptual responsibilities that may appear in an
Imaginarium-backed runtime.

Names are provisional while the ubiquitous language matures. This document does
not define Java classes, interfaces, APIs, storage, or implementation proposals.

## Runtime

The runtime is the conceptual execution environment for AI-assisted work.

It coordinates lifecycle, session boundaries, context assembly, registries,
provider/model access, planning support, supervision support, policies, skills,
capabilities, knowledge, and tools.

It is not Olorin. It is not a business domain service.

## Mission

A mission is a bounded objective.

It gives direction to planning, context selection, and execution support.

A mission is not automatically an executable plan and not automatically
authorization to act.

## Session

A session is a bounded interaction or execution window.

It keeps conversation, working context, selected capabilities, and policy context
from becoming accidentally global.

A session is not authentication and not necessarily durable.

## Context

Context is information made available to AI-assisted work.

It may include conversation, personality, role, capability, policy, user, mission,
and working context.

Context is not one memory bucket.

## Worker

Worker is current temporary terminology for a focused participant in AI-assisted
work.

A worker may carry out a bounded task or contribute a specialized capability under
runtime and policy boundaries.

The name is provisional. The concept should not be confused with a thread,
process, queue consumer, or direct business actor.

## Planner

A planner turns a mission into proposed steps, options, or structured intent.

Planning is proposal work. It does not approve privileged actions and does not
replace authorization.

## Supervisor

A supervisor observes runtime behavior.

Supervisors emit signals or events. They do not directly modify execution or
canonical domain state.

Default behavior should be conceptually equivalent to logging or no-op unless an
application explicitly reacts to supervision events.

## Policies

Policies describe conceptual boundaries around AI-assisted work.

They may guide allowed behavior, escalation, capability use, or supervision.

Policies are not a replacement for Custos domain authorization.

## Skills

Skills are reusable instruction or workflow knowledge packages.

They help a runtime or consuming system perform a type of task more consistently.

A skill is not the same as a tool and is not automatically execution authority.

## Capabilities

Capabilities are bounded forms of possible action or integration.

They describe what AI-assisted work may be able to do through the runtime.

A capability is not proof that a specific actor may perform a specific domain
operation. That remains an authorization question.

## Knowledge

Knowledge is structured or semi-structured grounding information.

It can help with interpretation, explanation, planning, and task quality.

Knowledge is not automatically memory and not automatically executable.

## Tools

Tools are concrete callable mechanisms.

They allow AI-assisted work to interact with systems or perform operations.

A tool is not a policy, not a skill, and not authorization by itself.

## Conceptual Relationship

```text
Mission
    ↓
Session
    ↓
Runtime
    ↓
Context + Policies + Capabilities + Skills + Knowledge + Tools
    ↓
Planner / Worker / Supervisor
```

This diagram is conceptual. It describes responsibilities, not implementation
structure.
