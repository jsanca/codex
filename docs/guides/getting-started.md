# Getting Started With Imaginarium

This guide is for engineers approaching Imaginarium before writing code.

It is not an API tutorial. It is a reading path and mental model guide.

## First Rule

Do not start with providers, frameworks, or classes.

Start with the conceptual model.

## Recommended Reading Order

1. Read [Imaginarium Philosophy](../PHILOSOPHY.md).
2. Read [Conceptual Architecture](../architecture/conceptual-architecture.md).
3. Read [AI Capability Continuum](../architecture/ai-capability-continuum.md).
4. Read [Codex AI Ubiquitous Language](../domain/glossary.md).
5. Read [Runtime Context Model](../architecture/context-model.md).
6. Read [Conceptual Runtime Model](../architecture/runtime-concepts.md).
7. Read [Supervision Model](../architecture/supervision.md).
8. Read [Agent Intelligence Levels](../architecture/intelligence-levels.md).
9. Read the Imaginarium ADRs in `docs/architecture/imaginarium/`.

## Concepts To Understand First

Before designing anything in Imaginarium, understand:

* Imaginarium is AI infrastructure, not the agent layer.
* Illuminarium can use AI without becoming agentic.
* Future agentic platforms consume Imaginarium rather than live inside it.
* Olorin is not the whole framework.
* Runtime context is composed of specialized contexts.
* Supervisors observe; they do not directly intervene.
* Capability does not equal authorization.
* Model size does not define agent responsibility.
* Agentic execution is optional.

## Common Mistakes To Avoid

Avoid starting with a provider API.

Avoid assuming every AI feature needs an agent.

Avoid putting business decisions inside Imaginarium.

Avoid treating memory as one global bucket.

Avoid treating tools as permissions.

Avoid treating supervisors as hidden execution controllers.

Avoid making Olorin the infrastructure framework.

Avoid designing Java APIs before the responsibility is stable.

## How To Approach New Ideas

When evaluating a new AI idea, ask:

1. Is this deterministic, generative, collaborative, or autonomous?
2. Does it belong to Illuminarium, a future Agent Platform, Olorin, or another
   consumer?
3. What part is reusable AI infrastructure?
4. What context does it need?
5. What capabilities or tools are involved?
6. What policies constrain it?
7. What should be observed?
8. What business domain remains responsible for the actual operation?

If the answer starts with a framework or provider name, step back. The concept
comes first.
