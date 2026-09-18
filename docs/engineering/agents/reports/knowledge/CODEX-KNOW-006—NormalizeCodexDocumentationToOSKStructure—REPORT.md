# Documentation Reconciliation Report: CODEX-KNOW-006

## Scope And Authority

**Task:** Normalize Codex documentation to the OSK structure.

**Mode:** Authorized documentation reconciliation. No production code, tests, runtime behavior,
or roadmap commitments were changed.

**Authorities reviewed:** [AGENTS.md](../../../../../AGENTS.md), [OSK.md](../../../../OSK.md),
the installed OSK knowledge-curation and engineering-reporting skills, and the
[CODEX-OSK-001 reality check](../../reviews/transversal/CODEX-OSK-001—CurrentStateRealityCheck—REVIEW.md).
The reality check is the evidence source for current-state corrections; historical task and report
content remains historical evidence.

## Before-State Summary

- `PROJECT.md` and the canonical roadmap were templates while active direction lived elsewhere.
- ADRs were split across `ADR-corpus/` and `future-forward/`, including conflicting legacy ADR
  numbers 008, 009, and 012.
- Agent tasks, reports, reviews, and checkpoints lived under `docs/agents/`; the OSK location
  `docs/engineering/agents/` contained only navigation files.
- Historical task specifications also remained under `docs/tasks/`.
- The module map and Custos checklist contained verified stale claims, including a nonexistent
  `RoleAssignmentPermissionsService` marked complete.

## Canonical OSK Structure

```text
docs/
  PROJECT.md
  OSK.md
  adr/
  architecture/
  domain/
  engineering/
    ENGINEERING_LOG.md
    agents/{tasks,reports,reviews,checkpoints,templates}/
  knowledge/
  roadmap/{ROADMAP.md,future/,history/}
  research/
  security/
```

`docs/adr/` is the sole canonical ADR corpus. `docs/engineering/agents/` is the sole canonical
agent-artifact tree. `docs/roadmap/ROADMAP.md` is the sole canonical roadmap.

## Migration Map

| Previous location | Canonical location | Action | Reason |
| --- | --- | --- | --- |
| `docs/ADR-corpus/` | `docs/adr/` | Moved ADR-004 and ADR-008 through ADR-012 | Formal decisions belong in the OSK ADR corpus. |
| `docs/future-forward/ADR-001` through `ADR-007` | `docs/adr/` | Moved and normalized filenames | They are distinct formal decision records. |
| `docs/future-forward/ADR-008`, `ADR-009`, and caching ADR-012 | `docs/roadmap/future/` | Reclassified and preserved | Legacy numbering conflicts with canonical ADRs; the documents remain proposals, not duplicate decisions. |
| `docs/agents/{tasks,reports,reviews,checkpoints,templates}` | `docs/engineering/agents/` | Moved | OSK canonical engineering evidence location. |
| `docs/agents/reviewer/` | `docs/engineering/agents/reviews/historical/` | Moved | Historical review artifact preserved without a parallel tree. |
| `docs/tasks/` | `docs/engineering/agents/tasks/legacy/` | Moved | Historical task specifications remain available but are not active work. |
| `docs/CODEX-ROADMAP-PHASES.md` | `docs/roadmap/history/` | Moved | Preserves phase history without competing with the canonical roadmap. |
| Root architecture, domain, philosophy, MVP, workflow, and module proposals | Semantic architecture, domain-history, or roadmap-future destinations | Moved | Placement now reflects purpose rather than original task chronology. |

## Duplicate Reconciliation

No conflicting ADR text was merged. The accepted/current corpus retained its distinct ADR numbers
in `docs/adr/`. The legacy future-forward documents that reused those identifiers are preserved
under `docs/roadmap/future/` with their original content and proposal status. The ADR index makes
the boundary explicit.

The former `docs/agents/` tree now contains only a compatibility README. All active navigation
points to `docs/engineering/agents/`.

## Stale Documentation Corrected

- `docs/security/CUSTOS-IMPLEMENTATION-CHECKLIST.md`: corrected the false completion claim for
  `RoleAssignmentPermissionsService`; corrected the secured-decorator phase status and stale
  AccessDecisionService follow-up wording.
- `docs/modules/MODULE-RESPONSIBILITIES.md`: corrected indexing ownership, module skeleton status,
  current Concilium secured composition, Custos responsibility, and marked Nuntius/Speculum as
  historical proposals rather than existing modules.
- `docs/architecture/CODEX-BLUEPRINT.md`: replaced stale Custos “in progress” wording with the
  verified completed/deferred split.
- `AGENTS.md` and `CLAUDE.md`: redirected Custos and calibration references to canonical paths.
- `docs/engineering/ENGINEERING_LOG.md`: added the missing CODEX-017 and CODEX-OSK-001 entries.

## Historical Material Preserved

- Working roadmap phases: `docs/roadmap/history/`.
- Earlier task specifications: `docs/engineering/agents/tasks/legacy/`.
- Earlier reviewer artifact: `docs/engineering/agents/reviews/historical/`.
- MVP domain model and architecture/lore sketches: `docs/domain/history/` and
  `docs/architecture/history/`.
- Non-adopted proposals: `docs/roadmap/future/`.

## Remaining Unresolved Items

- The future-forward Custos model remains historically valuable but overlaps with current
  `docs/security/CUSTOS-MODEL.md`; it is preserved as a proposal rather than merged.
- Several historical reports retain claims that were correct when written but are superseded by
  current evidence. They are not rewritten as contemporary records.
- Current durable knowledge is still concentrated in architecture, security, modules, and ADRs;
  `docs/knowledge/` remains an intentionally light entrypoint until a concept needs a dedicated
  canonical knowledge page.

## After-State Navigation

```text
AGENTS.md
  -> docs/PROJECT.md
  -> docs/roadmap/ROADMAP.md
  -> relevant docs/architecture, docs/security, docs/modules, or docs/knowledge page
  -> docs/adr/ for decision rationale
  -> docs/engineering/agents/ for task, report, review, or checkpoint evidence
```

## Validation

```text
git diff --check
```

The command passed. No tests were run because the task changed documentation only. No commit was
created.
