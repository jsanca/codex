# Task CODEX-KNOW-005 — Capture AI Architecture Risk Guidelines

Status: Planned
Owner: Elito
Role: Knowledge Curator
Target: 20–30 minutes
Hard Stop: 45 minutes

## Execution Requirements

Apply, if available:

* `.opencode/skills/osk-knowledge-curation/SKILL.md`
* `.opencode/skills/osk-engineering-reporting/SKILL.md`

No commits.

This is documentation-only.

Create report under:

* `docs/engineering/agents/reports/knowledge/`

## Objective

Capture the useful architectural guidance from Brio's AI architecture and legal risk mitigation research without changing the current Codex implementation roadmap.

The goal is to document future AI capability boundaries for Codex CMS while preserving the current focus on authorization decision records, bounded routing, Observance metrics, and future security audit design.

## Source Material

Review the provided research brief:

* `codex_cms_ai_architecture_guidelines.pdf`

Use it as a risk-guidance input, not as final legal advice.

## Context

Codex is currently working through the authorization visibility roadmap:

* ADR-012 — Authorization Decision Records and Bounded Security Audit Routing: DONE
* CODEX-018B — AccessDecision Trace Model Proposal: DONE
* CODEX-018C — Authorization Observance Metrics Design: DONE
* Next planned implementation slice: CODEX-018D — Local Authorization Decision Record Prototype

Brio's research highlights AI architecture risk controls that are relevant to future Codex AI capabilities, especially Olorin / AI agents / guided drafting / content assistance.

Important source themes:

* avoid unconstrained open-ended generative chat
* prefer bounded, task-specific workflows
* use grounded RAG with strict guardrails
* require HITL for publishing, legal, financial, or state-changing actions
* maintain auditability and traceability for AI interactions
* capture prompt, system context, model version, configuration, RAG sources, and human reviewer identity where appropriate
* use architecture, not prompting alone, as the main defense against hallucination and liability risk

## Scope

Create a concise future-guidance document or section that captures what is relevant for Codex.

Recommended location:

* If an AI or architecture guidance directory exists, use it.
* Otherwise, create a knowledge report only and recommend the correct future location.

Possible future document title:

* `AI Capability Risk Boundaries for Codex CMS`

Do not create a large compliance framework.

Do not modify runtime code.

Do not change current roadmap priorities.

## Required Content

Document these future principles:

### 1. Bounded AI workflows

Codex AI capabilities should prefer task-specific workflows over open-ended chat.

Examples of safer AI capabilities:

* content tagging
* metadata extraction
* structural validation
* guided drafting
* summarization with source grounding
* content quality checks
* policy checks
* accessibility checks

### 2. AI does not bypass Custos

AI agents must be treated as explicit actors.

They must not operate as invisible superusers.

Future AI actors must pass through Custos authorization boundaries.

### 3. Human-in-the-loop for risky actions

AI may propose, draft, validate, or recommend.

AI should not directly commit high-risk state-changing actions without explicit human approval.

HITL should be required for future actions such as:

* publishing content
* modifying content type schemas
* granting or revoking permissions
* changing workflow approvals
* producing legal or contractual text
* executing financial or regulated actions

### 4. Audit and visibility alignment

Document how future AI actions should map to existing Codex visibility concepts:

```text
Chronicon:
  domain facts that actually occurred

Aegis / Security Stream:
  security-relevant AI actions, denials, privileged use, approval decisions, suspicious attempts

Observance:
  aggregate PI-safe operational metrics

AccessDecision trace:
  explanation of authorization outcomes

AI interaction trace:
  prompt/context/model/source/reviewer lineage where policy requires it
```

If `Aegis` is not yet an accepted project name, mark it as a candidate name for the future security stream.

### 5. RAG and grounding

Future AI-generated content should be grounded in validated Codex/CMS data where possible.

Document that ungrounded AI output should not be treated as authoritative.

### 6. Output validation

Future AI output should pass through validators appropriate to the workflow.

Examples:

* schema validator
* policy validator
* factual grounding validator
* permission validator
* content safety validator
* legal/compliance validator if applicable

### 7. AI lineage

For future high-risk AI workflows, Codex should be able to record lineage such as:

* prompt or task instruction
* system context
* model/provider/version
* model configuration, where relevant
* RAG source references
* generated output
* validation outcome
* human reviewer identity
* approval/rejection outcome
* final committed domain action, if any

### 8. No roadmap derailment

Explicitly state that this guidance does not replace the current roadmap.

Current priority remains:

```text
CODEX-018D — Local Authorization Decision Record Prototype
```

This AI guidance is future-facing and should inform later Olorin / AI capability work.

## Out of Scope

Do not implement:

* Java code
* tests
* AI agents
* RAG
* guardrails
* validators
* HITL workflows
* Olorin behavior
* Chronicon changes
* Aegis/security stream changes
* Observance metrics
* legal/compliance policy engine

Do not make definitive legal claims beyond what is presented as risk guidance.

Do not treat the research brief as formal legal advice.

## Deliverables

Create:

* `docs/engineering/agents/reports/knowledge/CODEX-KNOW-005—CaptureAIArchitectureRiskGuidelines—REPORT.md`

Optionally update or create one concise future-facing guidance document if there is an obvious existing location.

The report should include:

1. Source reviewed
2. Key takeaways
3. Codex architecture implications
4. Future AI capability principles
5. Relationship to Custos, Chronicon, Aegis/security stream, Observance, and traces
6. Recommended future tasks
7. Confirmation that current roadmap remains unchanged

## Recommended Future Tasks

Suggest, but do not implement:

1. `CODEX-AI-001 — AI Capability Risk Boundaries Discovery`
2. `CODEX-AI-002 — Olorin Actor and Custos Boundary Model`
3. `CODEX-AI-003 — HITL Workflow Requirements for AI-Assisted Publishing`
4. `CODEX-AI-004 — AI Interaction Trace and Lineage Model`
5. `CODEX-AI-005 — RAG Guardrails and Output Validation Strategy`

## Acceptance Criteria

* The report captures the useful AI architecture risk guidance.
* The report clearly distinguishes architecture guidance from legal advice.
* The report states that AI actors must not bypass Custos.
* The report recommends bounded workflows over open-ended chat.
* The report documents HITL requirements for high-risk actions.
* The report maps future AI visibility to Chronicon, Aegis/security stream, Observance, and traces.
* The report does not change current roadmap priority.
* `git diff --check -- docs` passes.
* No commits performed.

## Validation

Run:

```bash
git diff --check -- docs
```

No tests required because this is documentation-only.
