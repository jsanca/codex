# Codex Imaginarium

Imaginarium is the AI integration layer of the Codex ecosystem.

Its purpose is to provide the infrastructure required for Codex to interact with language models, external AI services, orchestration libraries, and future machine-oriented integrations without making the core CMS or every module agentic by default.

## Position in the Codex Ecosystem

Imaginarium should be understood in relation to the other major parts of the system:

- **Imaginarium** is the AI integration layer.
- **Olorin** is the agent layer.
- **Codex CMS** is the operational domain where sites, content types, content items, assets, workflows, and related structures live.

This separation is intentional.

Imaginarium does not represent the agent itself. Instead, it provides the building blocks that an agent such as Olorin may use in order to reason, orchestrate, communicate with models, or interact with external AI-oriented systems.

## What Imaginarium Is Responsible For

Imaginarium is the place where Codex can integrate concerns such as:

- model connectivity,
- connectors to AI providers,
- orchestration primitives,
- prompt or context handling support,
- interoperability with external AI frameworks or protocols,
- and other infrastructure needed to support intelligent interactions.

The exact implementation may evolve over time, but the role remains the same: Imaginarium provides AI-enabling infrastructure rather than domain intent.

## What Imaginarium Is Not

Imaginarium should not be treated as the domain agent.

It should not own high-level business intent such as:

- deciding how to create a site,
- interpreting domain goals such as creating a pet store,
- planning multi-step content modeling actions,
- or reasoning about Codex capabilities at the business level.

Those responsibilities belong to the agent layer, represented by Olorin.

## Why This Separation Matters

Keeping Imaginarium separate from Olorin protects the architecture from unnecessary coupling.

This allows Codex to:

- adopt AI-related tooling without forcing agentic behavior everywhere,
- evolve its integration mechanisms independently from its domain reasoning model,
- support multiple future agent implementations if needed,
- and keep the CMS core focused on business structures rather than AI infrastructure.

In short, Imaginarium is the enabling substrate, not the acting intelligence.

## Current Intent

At this stage, Imaginarium is primarily a conceptual and integration-oriented module.

Its role is to define and eventually host the technical bridges that allow Codex to communicate with AI systems in a modular, controlled, and evolvable way.

## Related Documentation

For the complementary agent-facing vision, see the `README.md` and `architecture.md` documents in the `codex-olorin` module.
