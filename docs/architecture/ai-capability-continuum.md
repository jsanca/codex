# AI Capability Continuum

This document explains the progression from deterministic computation toward
autonomous execution in Codex.

It is conceptual. It does not define APIs, implementation classes, or provider
bindings.

## Principle

Not every AI feature requires agents.

Codex should use the minimum intelligence and autonomy required for the task.
Some capabilities need deterministic services. Some need generative services. Some
need collaboration. Only some need agentic or autonomous execution.

## Continuum

```text
Deterministic Services
    ↓
Generative Services
    ↓
Collaborative Intelligent Systems
    ↓
Autonomous Systems
```

## Deterministic Services

Deterministic services perform predictable work.

They may use fixed rules, transformations, validators, or known algorithms.

AI may not be needed at all. This is still important because good architecture
should not force AI into problems that can be solved directly.

## Generative Services

Generative services use AI to produce, transform, summarize, classify, or enrich
material.

They can be powerful without being agentic.

A summarizer, classifier, semantic tagger, metadata extractor, draft generator, or
translation helper may use Imaginarium infrastructure while remaining a normal
service.

This is where Illuminarium has a natural role.

## Collaborative Intelligent Systems

Collaborative intelligent systems interact with humans or other systems over time.

They may maintain sessions, adapt to context, ask clarifying questions, or propose
next steps.

They may use planners, policies, skills, tools, and supervision, but they still do
not automatically become autonomous.

## Autonomous Systems

Autonomous systems can pursue missions with less direct human step-by-step control.

They require stronger boundaries:

* explicit capabilities
* policy context
* supervision
* authorization checks
* clear mission scope
* observable behavior

Autonomy is optional and should not be the default assumption for AI features.

## Why Illuminarium Exists Separately

Illuminarium exists because enrichment and semantic enhancement are not the same
as agentic execution.

An article can be summarized, classified, annotated, embedded, or enriched without
creating a planner, supervisor, mission, or worker.

Illuminarium can consume Imaginarium infrastructure for generative and semantic
capabilities while staying focused on enrichment.

## Why Future Agentic Runtimes Are Separate

Agentic runtimes need concepts that enrichment services do not always need:

* missions
* sessions
* planners
* workers
* supervisors
* policies
* capability selection
* execution observation

Those concerns should not be forced into Illuminarium or into every AI feature.

Imaginarium provides shared infrastructure underneath both paths.
