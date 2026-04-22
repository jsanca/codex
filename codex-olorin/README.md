# Codex Olorin

Olorin is the agent layer of the Codex ecosystem.

Its purpose is not simply to connect Codex to language models, but to provide a domain-aware agent capable of understanding the Codex CMS model, planning meaningful actions, and operating through explicit capabilities instead of ad hoc automation.

## Position in the Codex Ecosystem

Olorin should be understood in relation to the other major parts of the system:

- **Olorin** is the agent.
- **Imaginarum** is the AI integration layer that provides connectors, orchestration primitives, and interoperability with external AI tooling.
- **Codex CMS** is the operational domain where sites, content types, content items, assets, workflows, and related structures live.

This separation is intentional. Codex is not meant to be agentic by default. Olorin is the component responsible for reasoning over the domain and interacting with it safely.

## What Olorin Should Eventually Enable

The long-term goal is to make Codex operable through natural language in a way that is structurally meaningful.

Examples include requests such as:

- create a site,
- create a content type,
- create a page-oriented content type,
- create content items,
- create assets and attach them,
- generate starter solutions such as a pet store, blog, documentation site, or other blueprint-driven setup.

These are not simple CRUD requests. They are domain goals that often require multiple coordinated actions, validation rules, and planning.

## Why Olorin Exists

A request such as:

> Create a pet store with a homepage, a catalog, services, contact information, demo assets, and sample content.

should not be treated as a single low-level mutation.

Instead, Olorin should be able to:

1. understand the request as a composed objective,
2. expand it into an execution plan,
3. invoke domain-level operations explicitly,
4. rely on descriptors, constraints, and blueprints,
5. and produce traceable results.

## Architectural Direction

Olorin depends on more than the raw domain entities. To be operational, it requires support from additional architectural layers:

- a **domain model** describing what exists,
- a **capability and descriptor model** describing what concepts mean and what can be done with them,
- a **command and orchestration model** describing how the system should act,
- and a **blueprint layer** for reusable composed solutions.

This makes Olorin explainable, extensible, and safer to evolve.

## External Agent Discovery

A long-term goal of Codex is to allow external agents to discover what a Codex-powered site or system does, what services it offers, and how it can be interacted with.

That means Codex should eventually become self-describing for machine consumers as well as humans.

Likely directions include:

- semantic publication through formats such as JSON-LD,
- structured capability manifests,
- and agent-facing operational exposure through protocols or patterns similar to MCP.

This would allow external assistants and agents to understand both the meaning of a site and the operations it exposes.

## Current Intent

At this stage, Olorin is primarily a conceptual and architectural module.

Its role is to define how an agent should exist within Codex:

- domain-aware,
- capability-driven,
- blueprint-friendly,
- explainable,
- and traceable.

## Related Documentation

For the broader architectural vision, see:

- `architecture.md`
