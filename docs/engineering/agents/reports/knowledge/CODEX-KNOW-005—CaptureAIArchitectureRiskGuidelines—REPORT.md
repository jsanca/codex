# Knowledge Report - CODEX-KNOW-005: Capture AI Architecture Risk Guidelines

## Task

`TaskCODEX-KNOW-005—CaptureAIArchitectureRiskGuidelines`

## Date

2026-08-13

## Owner

Elito

## Mode

Knowledge curation. Documentation-only.

## Scope

Captured future-facing AI architecture risk guidance for Codex CMS from Brio's research
brief and existing Codex architecture documents.

No Java code, tests, roadmap changes, AI agents, RAG implementation, guardrails,
validators, HITL workflows, Olorin behavior, Chronicon changes, security stream changes,
Observance metrics, or legal/compliance policy engine were implemented.

This report is architecture guidance, not legal advice. The source brief is treated as
risk-guidance input. Formal legal, regulatory, insurance, or compliance conclusions still
require qualified review.

## Sources Reviewed

* `docs/research/codex_cms_ai_architecture_guidelines.pdf`
* `docs/engineering/agents/tasks/knowledge/TaskCODEX-KNOW-005—CaptureAIArchitectureRiskGuidelines.md`
* `docs/architecture/CODEX-BLUEPRINT.md`
* `docs/roadmap/history/CODEX-ROADMAP-PHASES.md`
* `docs/roadmap/future/custos-domain-authorization-model.md`
* `docs/adr/ADR-012-authorization-decision-records.md`
* `docs/engineering/agents/reports/knowledge/CODEX-018A—AuthorizationDecisionRecordsAndBoundedSecurityAuditRouting—REPORT.md`
* `docs/engineering/agents/reports/knowledge/CODEX-018B—AccessDecisionTraceModelProposal—REPORT.md`
* `docs/engineering/agents/reports/knowledge/CODEX-018C—AuthorizationObservanceMetricsDesign—REPORT.md`
* `docs/architecture/ai-capability-continuum.md`
* `docs/architecture/conceptual-architecture.md`
* `docs/architecture/imaginarium/ADR-001-Imaginarium-Owns-AI-Infrastructure.md`
* `docs/architecture/imaginarium/ADR-002-Separation-Between-Imaginarium-and-Olorin.md`

## Source Summary

The PDF was reviewed from the repository copy under `docs/research`. `pdfinfo` confirmed
the brief is a 3-page, unencrypted PDF, and all pages were rendered with `pdftoppm` for
visual inspection.

The PDF frames AI capability design for Codex CMS as a production architecture risk problem,
not a prompt-writing problem. It emphasizes that organizations can be accountable for
AI-generated commitments or misinformation, that insurance and regulatory exposure may be
material, and that AI systems should be designed with guardrails, auditability, and explicit
liability containment from the architecture phase.

Key recommendations in the brief:

* Move from unconstrained open-ended chat to bounded workflows.
* Use retrieval-augmented generation grounded in validated CMS data.
* Add strict guardrails and verification layers before user-visible output.
* Require human approval gates for state-changing, legal, financial, or contractual actions.
* Record AI input/output lineage, including prompt, context, model version/configuration,
  grounding sources, and reviewer identity where appropriate.
* Treat architecture, not prompting alone, as the primary defense against hallucination and
  operational/legal risk.

## Key Takeaways

### 1. Bounded workflows should be the default

Future Codex AI capabilities should prefer task-specific workflows over open-ended chat.
This matches the existing Imaginarium direction: AI infrastructure is reusable capability
support, not an unbounded agent with direct domain authority.

Safer AI capability examples:

* content tagging
* metadata extraction
* structural validation
* guided drafting
* summarization with source grounding
* content quality checks
* policy checks
* accessibility checks

Open-ended chat may still exist later, but it should not be the privileged default path for
domain mutation, publishing, schema changes, or security administration.

### 2. AI must not bypass Custos

Future AI actors must be explicit actors in the Codex domain model. They must not operate as
invisible superusers, hidden service accounts, or authority shortcuts.

Current Custos direction already supports this:

* Olorin and other agent actors are explicit `AgentActor` concepts.
* Agent actors must not manage permissions.
* Olorin may explain, propose, compare, or prepare permission changes.
* Olorin must not approve or apply privileged permission changes by its own authority.
* Future AI actors should pass through Custos authorization boundaries like other actors.

This report does not change Custos semantics. It preserves the existing rule that AI is not
a security authority.

### 3. HITL is required for risky actions

AI may propose, draft, validate, summarize, or recommend. For high-risk actions, final
commit should require explicit human approval or an equivalent trusted non-agent policy path
defined later.

Future HITL-required categories should include:

* publishing content
* modifying content type schemas
* granting or revoking permissions
* assigning or revoking roles
* changing workflow approvals
* producing legal or contractual text
* executing financial or regulated actions

This is especially important for Olorin-style proposal flows. The AI may prepare a plan, but
the final authority should remain outside the AI actor.

### 4. Grounding is a risk boundary

Future AI-generated content should be grounded in validated Codex/CMS data where possible.
Ungrounded output should not be treated as authoritative.

RAG should be used as a controlled grounding mechanism, not as an unrestricted storage
scraper. Retrieval should expose knowledge concepts and source references that can be
validated, traced, and presented back to reviewers.

This aligns with the existing knowledge-routing direction:

* retrieval is independent from reasoning
* deterministic retrieval is preferred when sufficient
* expensive or agentic exploration is an escalation path, not the default
* context assembly optimizes quality and computational cost

### 5. Validation belongs around AI output

Future AI output should pass through validators appropriate to the workflow before it becomes
user-visible or domain-committed.

Candidate validator categories:

* schema validator
* policy validator
* factual grounding validator
* permission validator
* content safety validator
* legal/compliance validator, if applicable

These validators are future work. This report does not define their APIs, storage, runtime
composition, or enforcement behavior.

## Codex Architecture Implications

### Imaginarium

Imaginarium remains AI infrastructure. The guidance reinforces that Imaginarium should
provide reusable support for models, providers, context, grounding, tools, runtime concepts,
and future agent infrastructure without becoming the business agent layer itself.

### Olorin

Olorin remains a future domain-aware assistant/planner concept. This guidance strengthens
the existing boundary:

* Olorin may propose.
* Olorin may draft.
* Olorin may explain.
* Olorin may prepare approval plans.
* Olorin must not silently commit privileged or high-risk actions.
* Olorin must not bypass Custos.

### Illuminarium

Illuminarium remains justified as a separate semantic/enrichment layer. Not every AI feature
requires an agent. Many bounded tasks, such as summarization, tagging, enrichment, and
quality checks, may use Imaginarium infrastructure without using Olorin-style autonomous
execution.

### Custos

Custos remains the authorization boundary for future AI actors. AI actions should be
evaluated as domain operations against permissions, scopes, and explicit actors.

Important implications:

* no invisible AI superusers
* no direct permission-management authority for agent actors
* no bypass around `AccessDecisionService`
* privileged proposal/approval flows remain future work
* authorization traces remain separate from AI interaction traces

### Chronicon

Chronicon should continue to represent domain facts that actually occurred. AI proposals,
drafts, denied attempts, and failed validations are not automatically domain facts.

If an AI-assisted action results in a real domain event, Chronicon may later record the
committed domain fact through the normal domain audit path. AI-specific lineage or security
facts should not be forced into Chronicon's current domain audit stream by default.

### Aegis / Security Stream

`Aegis` remains only a candidate name for a future security stream. It is not an accepted
module or committed architecture name in this report.

Future security audit should be considered for security-relevant AI facts such as:

* AI authorization denials
* privileged AI-assisted operations
* suspicious prompt-injection attempts
* approval decisions
* role/grant proposal attempts
* policy violations
* invariant violations

The storage mechanism remains undecided. It could be a dedicated stream, a dedicated audit
category, a Chronicon-backed security audit store, or a pluggable sink.

### Observance

Observance may receive aggregate PI-safe operational metrics for future AI workflows.

Safe metric categories may include:

* AI workflow attempts total
* AI workflow success/failure total
* validation failures by safe category
* grounding confidence buckets
* HITL required/approved/rejected counts
* provider latency
* model call failures
* security-relevant denial counts by safe category

Observance must not receive sensitive prompts, full generated text, actor internals,
resource identifiers, permission topology, or high-cardinality user/resource values.

### AccessDecision Trace

AccessDecision trace remains focused on authorization outcomes: why an actor was granted or
denied a permission over a resource/scope.

AI interaction trace is different. It should capture AI execution lineage where policy
requires it. The two traces may be correlated in future workflows, but they are not the same
record.

## Visibility Alignment

```text
Chronicon:
  domain facts that actually occurred

Aegis / Security Stream:
  security-relevant AI actions, denials, privileged use,
  approval decisions, suspicious attempts

Observance:
  aggregate PI-safe operational metrics

AccessDecision trace:
  explanation of authorization outcomes

AI interaction trace:
  prompt/context/model/source/reviewer lineage where policy requires it
```

## AI Lineage Guidance

For future high-risk AI workflows, Codex should be able to record lineage such as:

* prompt or task instruction
* system context
* model provider, model name, and model version
* model configuration, where relevant
* RAG source references
* generated output
* validation outcome
* human reviewer identity
* approval or rejection outcome
* final committed domain action, if any

Lineage should be policy-driven. Low-risk internal assistance may not need the same level of
durable trace as publishing, schema mutation, security administration, legal, financial, or
regulated workflows.

## Future AI Capability Principles

1. Prefer bounded workflows over unconstrained chat.
2. Use the minimum intelligence required for the task.
3. Ground generated output in validated Codex/CMS knowledge where possible.
4. Treat ungrounded generated output as non-authoritative.
5. Validate output before display or commit.
6. Require HITL for high-risk or state-changing actions.
7. Treat AI systems as explicit actors, not hidden authorities.
8. Route future AI actions through Custos authorization boundaries.
9. Separate domain audit, security audit, operational metrics, authorization trace, and AI
   lineage.
10. Preserve provider independence and avoid provider-shaped domain architecture.

## Relationship To Current Roadmap

This report does not replace or reorder the current roadmap.

Current priority remains:

```text
CODEX-018D - Local Authorization Decision Record Prototype
```

The AI guidance is future-facing. It should inform later Olorin, Imaginarium, Illuminarium,
RAG, HITL, validation, and AI interaction trace work, but it should not derail the current
authorization visibility sequence.

## Recommended Future Tasks

Suggested future tasks, not implemented here:

1. `CODEX-AI-001 - AI Capability Risk Boundaries Discovery`
2. `CODEX-AI-002 - Olorin Actor and Custos Boundary Model`
3. `CODEX-AI-003 - HITL Workflow Requirements for AI-Assisted Publishing`
4. `CODEX-AI-004 - AI Interaction Trace and Lineage Model`
5. `CODEX-AI-005 - RAG Guardrails and Output Validation Strategy`

## Out Of Scope Confirmed

This task did not implement:

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
* roadmap priority changes

## Validation

```bash
git diff --check -- docs
```

Result: passed.

No tests are required because this is documentation-only.

## Unresolved Questions

* Where should long-lived AI risk guidance eventually live: Imaginarium architecture docs,
  a future AI capability boundary ADR, or an Olorin-specific guidance document?
* Should `Aegis` become the accepted name for the future security stream, or remain a
  placeholder until the boundary is implemented?
* Which AI lineage fields should be mandatory by workflow category?
* Which validator categories should be required before publishing, schema mutation, and
  security administration workflows?

## No Commits

No commits were created.
