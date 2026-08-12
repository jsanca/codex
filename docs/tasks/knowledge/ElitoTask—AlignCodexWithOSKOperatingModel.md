Elito Task — Align Codex with OSK operating model

Context:
Codex has organically adopted the OSK-style multi-agent operating model:

* Clio implements production code and tests.
* Deep reviews architecture, behavior preservation, and risk.
* Elito maintains documentation and architectural memory.
* Brio performs research.
* Elo coordinates direction, task slicing, and architectural judgment.
* Jonathan owns final decisions.

This model already appears in CODEX-ROADMAP-PHASES.md, but Codex should now explicitly align with OSK vocabulary, checkpoints, and workflow discipline.

Goal:
Create or update documentation that explains how Codex uses the OSK operating model without coupling Codex product architecture to OSK internals.

Scope:
Documentation only.

Suggested files:

* docs/operations/CODEX-OSK-ALIGNMENT.md
* CODEX-ROADMAP-PHASES.md, only if a small reference is useful
* AGENTS.md / CLAUDE.md, only if necessary

Do not modify:

* production code
* tests
* module-info.java
* Custos implementation

Document the following:

1. Codex as product vs Codex as OSK proving ground

Clarify:

* Codex product architecture remains domain-driven and module-driven.
* OSK governs how the work is coordinated, reviewed, documented, and remembered.
* Codex core modules must not depend on OSK runtime concepts.

2. Agent responsibilities

Include:

* Clio builds.
* Deep audits.
* Elito explains.
* Brio researches.
* Elo keeps the thread.
* Jonathan decides.

3. Work artifact vocabulary

Define:

* Phase
* Task / Slice
* Checkpoint
* Review
* Report
* ADR
* Skill
* Roadmap update

4. Workflow rules

Capture:

* small reviewed slices
* implementation and review are separate
* documentation follows completed architectural movement
* roadmap is living but not noisy
* research is advisory until accepted
* architectural decisions require Jonathan approval

5. Checkpoint discipline

Define when to create checkpoints:

* after completing a phase
* after completing a major secured decorator / subsystem
* after an ADR-worthy decision
* before switching projects for a while
* after Deep finds a significant gap

6. Relationship to OSK skills

Explain:

* Codex may use OSK skills for boundary review, roadmap sync, documentation review, and architecture review.
* Skills guide agents but do not replace human architectural judgment.
* Skills should be referenced in reports when used.

7. Immediate recommendation

Add a recommendation that the current Custos Phase 1.4 completion should produce a checkpoint:

* CHECKPOINT-CODEX-CUSTOS-SECURED-DECORATORS.md

Expected output:

* files created/updated
* short summary of OSK alignment decisions
* any proposed follow-up tasks
