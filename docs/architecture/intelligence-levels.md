# Agent Intelligence Levels

This note captures the current discussion around classifying agent intelligence in
Codex.

The terminology is still evolving. This document is conceptual and does not define
implementation classes, Java APIs, or product tiers.

## Principle

Agent intelligence should be classified by architectural responsibility, not by
model size.

A larger model does not automatically make an agent supervisory, strategic, or
safer. A smaller model may still participate in a higher-responsibility workflow
if the surrounding runtime, policy, and supervision model support that role.

## Deterministic Agents

Deterministic agents follow predefined rules or bounded procedures.

Responsibility:

* execute predictable transformations
* apply known rules
* avoid open-ended interpretation

Typical boundary:

* useful for repetitive support tasks
* not responsible for broad planning or supervision

## Reactive Agents

Reactive agents respond to immediate input or events.

Responsibility:

* interpret a local trigger
* produce a local response
* act within the current context

Typical boundary:

* does not own long-range planning
* should not infer broad mission authority

## Specialist Agents

Specialist agents focus on a narrow domain or capability area.

Responsibility:

* apply focused expertise
* use domain-specific knowledge
* support a bounded kind of task

Typical boundary:

* should not become a general orchestrator
* should operate through explicit capabilities and policies

## Planning Agents

Planning agents transform missions into possible plans, steps, or proposals.

Responsibility:

* decompose objectives
* compare options
* propose structured next actions

Typical boundary:

* planning is not approval
* planning is not execution authority
* plans may require human, policy, or domain validation before action

## Supervisory Agents

Supervisory agents observe runtime behavior and emit supervision signals.

Responsibility:

* detect loops, drift, or risk patterns
* evaluate mission progress
* emit events for application-level reaction

Typical boundary:

* supervisors are observers, not business actors
* they do not directly mutate execution or domain state

## Current Direction

The current vocabulary favors responsibility-based categories:

* deterministic agents
* reactive agents
* specialist agents
* planning agents
* supervisory agents

These terms may be refined as Codex learns more. The stable decision is the
classification principle: responsibility matters more than model size.
