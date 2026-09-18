# Use Case — ContentItem Restore Under Secured Runtime

Status: Draft
Source of truth: `intent.md`
Scope: Custos C1 — Complete Secured Mutation Enforcement

## Summary

Establish the observable authorization behavior for the existing ContentItem restore domain operation when invoked through the secured Codex runtime. Restore must produce an explicit grant/deny decision rather than a fail-closed placeholder, while preserving the domain's existing rule that restore returns an archived item to `DRAFT`.

## Actors

- **Primary actor**: a caller attempting to restore an archived ContentItem back to `DRAFT`.
- **Authorization system**: Custos, evaluating the caller's authority against the target ContentItem.
- **Domain system**: the Codex core kernel that owns the ContentItem lifecycle.
- **Excluded actor**: `AgentActor` must never carry permission-management authority for this operation.

## Preconditions

- The ContentItem referenced by the request exists in the domain.
- The ContentItem is currently in `ARCHIVED` status (restore is meaningful only from `ARCHIVED`).
- The ContentItem belongs to a Site and a ContentType that are known to the runtime.
- The caller is a well-formed Actor recognizable by Custos.
- A permission resolution snapshot exists for the current evaluation.
- The runtime under exercise is the secured composition.

## Trigger

The caller invokes the ContentItem restore operation exposed by the secured runtime for a specific archived item.

## Main Flow — Authorized

1. The secured boundary receives the restore request for the target ContentItem.
2. Custos evaluates the caller's authority for a restore-shaped permission at a resource scope that includes the target ContentItem.
3. Custos returns a granted decision.
4. The domain restore operation is invoked with the original arguments.
5. The ContentItem transitions from `ARCHIVED` to `DRAFT`, per the existing state machine.
6. Whatever downstream events, projections, and audit records that normally accompany a successful restore occur exactly as they would outside the secured runtime.

## Alternate Flow — Unauthorized

1. The secured boundary receives the restore request.
2. Custos evaluates the caller's authority and returns a denied decision.
3. The secured boundary raises the standard Custos denial signal used by every other secured mutation.
4. The domain restore operation is not invoked.
5. No domain event, projection update, or Chronicon audit record attributable to this denied attempt is produced.
6. The ContentItem remains in `ARCHIVED` status.

## Alternate Flow — Domain Precondition Not Met

1. The caller invokes restore against a ContentItem that is not `ARCHIVED`.
2. Authorization is evaluated normally.
3. If Custos denies, the denial path applies as above.
4. If Custos grants, the domain restore operation itself refuses per the existing state-machine rule; the observable outcome is the domain's own precondition error, not a Custos denial. This is out of scope for Custos to change.

## Alternate Flow — Hard Invariant Violation

1. Evaluation surfaces a Custos hard invariant violation.
2. The violation propagates as a fatal Custos error and is not converted into a denial.
3. The domain restore operation is not invoked.

## Boundaries

- Authority evaluated on ContentItem X must not, by itself, permit restoring unrelated ContentItem Y.
- Authority must respect the existing scope walk (`ContentItem → ContentType → Site → Global`); no scope layer is added or reordered.
- Permission lookup for this operation must never cross Site boundaries.
- Custos does not redefine the domain rule that restore returns the item to `DRAFT` (not `PUBLISHED`).
- Restore is distinct from publish: authority to restore must not implicitly grant authority to publish, and vice versa.
- Out of scope: role administration, direct actor grants, decision records, decision traces, metrics, persistence, collection-read authorization.

## Postconditions

### After an authorized call
- The ContentItem is in `DRAFT` status.
- The same downstream effects that follow a normal restore occur.
- The observable outcome is indistinguishable from invoking restore through the unsecured runtime with a valid caller.

### After a denied call
- The ContentItem remains in `ARCHIVED` status.
- No restore-related event, projection, or audit record exists for this attempt.
- The caller receives the standard Custos denial signal.

### After a precondition failure on a granted call
- The ContentItem is unchanged.
- Any resulting error is a domain precondition error, not a Custos denial.

### After an invariant violation
- No domain state changed.
- The fatal signal is not swallowed or downgraded.

## Observable Verifications

- A caller with sufficient authority can restore an archived ContentItem to `DRAFT`.
- A caller without sufficient authority cannot restore an archived ContentItem, and its status remains `ARCHIVED` afterward.
- A denied restore attempt produces none of the side effects that a successful restore would.
- Authority scoped to one ContentItem, ContentType, or Site does not enable restore on an unrelated resource.
- Authority to restore does not, by itself, cause the item to become `PUBLISHED`.
- Authority granted at a broader scope may permit restore of items within that scope, per the existing scope walk — but not across Site boundaries.
- The secured runtime never falls back to an "unsupported operation" style refusal for this operation.

## Open Questions (Deferred to Design)

- Whether restore authority should align with update authority, archive authority, or be treated as its own capability.
- Which built-in roles receive restore authority, if any.
- Whether restore authority is meaningful at broader scopes or should be constrained.
- Whether authority to archive implies authority to restore, or the two are strictly independent.
- Exact permission constants, method names, and identifiers.

These belong to design and are intentionally not answered here.
