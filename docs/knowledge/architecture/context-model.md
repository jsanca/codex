# Runtime Context Model

This note documents the conceptual separation between runtime contexts in
Imaginarium.

It is documentation only. It does not define storage mechanisms, Java APIs, or
implementation classes.

## Purpose

Recent design discussion clarified that "memory" is too broad to describe every
kind of information used by an AI runtime.

The Agent Runtime needs a more precise vocabulary for context so future designs
can reason about what information is being used, why it is present, and how it is
bounded.

## Context Is Composed

Runtime context is not one object and not one persistence store.

Conceptually, a runtime may assemble several specialized contexts:

* conversation context
* personality context
* role context
* capability context
* policy context
* user context
* mission context
* working context

These contexts may overlap in what they influence, but their responsibilities
should remain distinct.

## Conversation Context

Conversation context captures the interaction history relevant to the current
session.

Responsibility:

* preserve local continuity
* carry recent user intent
* support references to earlier turns

What it is not:

* not long-term memory by default
* not user identity
* not authorization

## Personality Context

Personality context describes the desired interaction style, tone, or behavioral
presence for the AI-assisted system.

Responsibility:

* shape communication style
* maintain interaction consistency
* preserve user-facing persona boundaries

What it is not:

* not permission policy
* not domain authority
* not a substitute for task instructions

## Role Context

Role context describes the conceptual role the AI-assisted system is currently
playing.

Responsibility:

* clarify responsibility
* constrain the kind of help being provided
* distinguish implementer, reviewer, documenter, planner, or supervisor modes

What it is not:

* not a security role
* not a Custos Role
* not a permission grant

## Capability Context

Capability context describes what the AI-assisted system can access or invoke in
the current setting.

Responsibility:

* expose allowed capabilities
* support tool or skill selection
* keep runtime behavior bounded

What it is not:

* not proof that an action is authorized
* not a domain permission model
* not a business operation

## Policy Context

Policy context describes constraints, safety boundaries, and escalation rules.

Responsibility:

* shape allowed behavior
* identify actions that require approval
* preserve boundaries between proposal and execution

What it is not:

* not the same as Custos authorization
* not a replacement for domain service checks
* not a role blueprint

## User Context

User context describes information about the human or external party involved in
the session when such information is relevant.

Responsibility:

* support personalization where appropriate
* preserve user intent
* distinguish user needs from system or agent goals

What it is not:

* not authentication
* not necessarily a persisted profile
* not authorization by itself

## Mission Context

Mission context describes the objective being pursued.

Responsibility:

* preserve the current goal
* support planning
* provide criteria for relevance

What it is not:

* not an executable workflow by itself
* not a permission grant
* not persistent task storage

## Working Context

Working context contains transient material used while solving a problem.

Responsibility:

* hold intermediate reasoning artifacts
* track local assumptions
* support task progress within a bounded execution

What it is not:

* not durable knowledge by default
* not audit history
* not canonical domain state

## Memory and Knowledge

Memory and knowledge remain useful terms, but they should not erase the distinction
between specialized contexts.

Memory means retained information when a design explicitly needs retention.
Knowledge means information that grounds understanding. Neither term should be
used as a catch-all for every runtime input.
