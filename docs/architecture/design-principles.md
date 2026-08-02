# Imaginarium Design Principles

This document captures architectural principles emerging from the Imaginarium
design discussions.

It is conceptual. It does not define APIs, implementation classes, or a roadmap.

## Infrastructure Before Orchestration

Codex should understand the shared AI infrastructure before designing complex
agent orchestration.

Provider access, model abstraction, runtime context, capabilities, policies,
skills, tools, knowledge, and supervision vocabulary should be clear before higher
agentic behavior is designed.

## Composition Over Specialization

Imaginarium should provide composable concepts that different consumers can use.

Illuminarium, Olorin, or a future Agent Platform may need different combinations
of runtime concepts. Imaginarium should not collapse into one specialized agent.

## Minimum Intelligence Required

Use the least autonomous form that solves the problem.

Deterministic services, generative services, collaborative systems, and autonomous
systems are different levels of responsibility. Architecture should not make every
AI feature agentic by default.

## Observation Before Intervention

Supervisors observe and emit signals.

They do not secretly alter execution or mutate business state. Application-level
reaction to supervision must be explicit.

## Business Domains Should Not Know Provider Implementations

Codex domain modules should not know which provider or model supplies AI
capability.

Provider concerns belong below Imaginarium. Business behavior belongs above or
beside Imaginarium in the appropriate domain layer.

## Responsibilities Before APIs

Name the responsibility before naming an API.

Runtime, mission, session, context, policy, skill, capability, knowledge, tool,
planner, worker, and supervisor should be understood as responsibilities first.

## APIs Emerge From Concepts

An API should appear only after the concept is stable enough to teach.

If a name is still provisional, the API should wait.

## Agentic Execution Is Optional

Agentic execution is one possible consumer of Imaginarium, not the purpose of
Imaginarium itself.

Summarization, classification, enrichment, and semantic analysis may use
Imaginarium without using agents.

## Custos Remains the Authorization Gate

AI infrastructure does not grant itself domain authority.

Any future execution path that touches protected Codex operations must still pass
through the appropriate authorization boundary.
