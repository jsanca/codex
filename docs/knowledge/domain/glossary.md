# Codex AI Ubiquitous Language

This glossary captures the current conceptual language around Imaginarium and
future agentic work in Codex.

The terminology is intentionally conceptual. It does not define implementation
classes, Java APIs, persistence, or a roadmap.

## Agent

Definition: Temporary term for an AI-assisted actor-like process that may reason,
plan, observe, or help execute work through allowed capabilities.

Responsibility: Expresses a unit of AI-assisted behavior at a human-understandable
level.

What it is not: Not automatically a security authority, not a direct database actor,
and not synonymous with Imaginarium.

Related concepts: Mission, Runtime, Capability, Olorin, Supervisor.

## Mission

Definition: A bounded objective or task that gives direction to AI-assisted work.

Responsibility: Provides purpose and constraints for planning, context selection,
and execution support.

What it is not: Not a workflow engine, not a permission grant, and not necessarily
an executable plan.

Related concepts: Planner, Session, Context, Policy.

## Planner

Definition: A conceptual component or role that turns a mission into proposed
steps, options, or structured intent.

Responsibility: Helps shape work before execution.

What it is not: Not the runtime itself, not a supervisor, and not a business
authority.

Related concepts: Mission, Orchestrator, Runtime, Olorin.

## Orchestrator

Definition: A conceptual coordinator of high-level runtime flow.

Responsibility: Determines how work moves between mission, planning, runtime, and
other conceptual responsibilities.

What it is not: Not a business authority, not the runtime itself, and not a
supervisor.

Related concepts: Planner, Runtime, Supervisor, Mission.

## Supervisor

Definition: A conceptual observer of runtime behavior.

Responsibility: Watches execution, emits supervision events, and supports external
reaction by applications.

What it is not: Not a business actor, not an executor, and not a hidden modifier of
runtime behavior.

Related concepts: Runtime, Orchestrator, Loop Guard, Execution Observer.

## Runtime

Definition: The conceptual execution environment for AI-assisted work.

Responsibility: Manages lifecycle, sessions, contexts, registries, planning support,
model/provider abstraction, and supervision infrastructure.

What it is not: Not Olorin, not a domain service, and not a persistence mechanism.

Related concepts: Session, Context, Registry, Provider, Model.

## Session

Definition: A bounded interaction context for AI-assisted work.

Responsibility: Groups conversation, working state, selected capabilities, and other
runtime context for a specific interaction or execution window.

What it is not: Not authentication, not user management, and not necessarily durable.

Related concepts: Runtime, Conversation Context, Mission Context, Working Context.

## Context

Definition: Information made available to AI-assisted work for interpretation,
planning, or execution support.

Responsibility: Carries relevant information while preserving conceptual separation
between different kinds of runtime knowledge.

What it is not: Not a single global memory bucket and not automatically persistent.

Related concepts: Memory, Knowledge, Policy Context, Capability Context.

## Capability

Definition: A permitted kind of action or integration exposed to AI-assisted work.

Responsibility: Provides a bounded way for an agentic system to interact with Codex
or external infrastructure.

What it is not: Not a permission decision by itself and not a bypass around Custos.

Related concepts: Tool, Skill, Policy, Custos.

## Skill

Definition: A reusable package of instructions, workflow knowledge, or behavior
that can help an AI-assisted system perform a kind of task.

Responsibility: Encapsulates task-specific guidance or reusable procedure.

What it is not: Not automatically executable authority and not the same as a tool.

Related concepts: Capability, Tool, Knowledge, Registry.

## Policy

Definition: A conceptual rule or boundary that constrains AI-assisted behavior.

Responsibility: Describes what should be allowed, denied, escalated, observed, or
handled carefully.

What it is not: Not the same as a role blueprint and not a replacement for Custos
domain authorization.

Related concepts: Capability, Supervisor, Custos, Context.

## Tool

Definition: A callable function, connector, or mechanism that performs a concrete
operation.

Responsibility: Gives AI-assisted work a concrete interaction point.

What it is not: Not a policy, not a skill, and not authorization by itself.

Related concepts: Capability, Registry, Provider.

## Provider

Definition: A source of AI model access or AI-related infrastructure.

Responsibility: Supplies model execution or similar infrastructure capability.

What it is not: Not the model itself, not Codex domain logic, and not the runtime.

Related concepts: Model, Runtime, Registry.

## Model

Definition: An AI model available through a provider or local mechanism.

Responsibility: Performs model-level inference or generation.

What it is not: Not the architectural intelligence level of an agent and not the
source of domain authority.

Related concepts: Provider, Runtime, Intelligence Levels.

## Registry

Definition: A catalog of available runtime concepts such as capabilities, tools,
skills, providers, models, or policies.

Responsibility: Supports selection and discovery.

What it is not: Not a permission system by itself and not necessarily dynamic or
persistent.

Related concepts: Capability, Skill, Tool, Provider, Model.

## Memory

Definition: Remembered information that may help future interpretation or work.

Responsibility: Represents retained state or learned context when a future design
explicitly calls for it.

What it is not: Not a single catch-all concept for every kind of context and not
defined here as a persistence mechanism.

Related concepts: Context, Knowledge, Session, Working Context.

## Knowledge

Definition: Structured or semi-structured information used to understand a domain,
task, user intent, or operating environment.

Responsibility: Provides semantic grounding for planning, explanation, and
interpretation.

What it is not: Not necessarily memory, not necessarily user-specific, and not
automatically executable.

Related concepts: Context, Skill, Memory, Registry.
