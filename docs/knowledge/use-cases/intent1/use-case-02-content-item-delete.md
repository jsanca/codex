# Use Case — ContentItem Delete Under Secured Runtime

Status: Draft
Source of truth: `intent.md`
Scope: Custos C1 — Complete Secured Mutation Enforcement

## Summary

Establish the observable authorization behavior for the existing ContentItem delete domain operation when it is invoked through the secured Codex runtime. Delete must produce an explicit grant/deny decision rather than a fail-closed placeholder, while preserving the domain's existing precondition that delete requires `ARCHIVED` status.

## Actors

- **Primary actor**: a caller attempting to delete an archived ContentItem.
- **Authorization system**: Custos, evaluating the caller's authority against the target ContentItem.
- **Domain system**: the Codex core kernel that owns the ContentItem lifecycle.
- **Excluded actor**: `AgentActor` must never carry permission-management authority for this operation.

## Preconditions

- The ContentItem referenced by the request exists in the domain.
- The ContentItem is currently in `ARCHIVED` status (delete requires `ARCHIVED`).
- The ContentItem belongs to a Site and a ContentType that are also known to the runtime.
- The caller is a well-formed Actor recognizable by Custos.
- A permission resolution snapshot exists for the current evaluation.
- The runtime under exercise is the secured composition.

## Trigger

The caller invokes the ContentItem delete operation exposed by the secured runtime for a specific item.

## Main Flow — Authorized

1. The secured boundary receives the delete request for the target ContentItem.
2. Custos evaluates the caller's authority for a delete-shaped permission at a resource scope that includes the target ContentItem.
3. Custos returns a granted decision.
4. The domain delete operation is invoked with the original arguments.
5. The ContentItem is removed per existing domain semantics.
6. Whatever downstream events, projections, and audit records that normally accompany a successful delete occur exactly as they would outside the secured runtime.

## Alternate Flow — Unauthorized

1. The secured boundary receives the delete request.
2. Custos evaluates the caller's authority and returns a denied decision.
3. The secured boundary raises the standard Custos denial signal used by every other secured mutation.
4. The domain delete operation is not invoked.
5. No domain event, projection update, or Chronicon audit record attributable to this denied attempt is produced.
6. The ContentItem continues to exist and its status is unchanged.

## Alternate Flow — Domain Precondition Not Met

1. The caller invokes delete against a ContentItem that is not `ARCHIVED`.
2. Authorization is evaluated normally (a granted decision does not bypass domain preconditions).
3. If Custos denies, the denial path applies as above.
4. If Custos grants, the domain delete operation itself refuses per the existing state-machine rule; the observable outcome is the domain's own precondition error, not a Custos denial. This is out of scope for Custos to change.

## Alternate Flow — Hard Invariant Violation

1. Evaluation surfaces a Custos hard invariant violation.
2. The violation propagates as a fatal Custos error and is not converted into a denial.
3. The domain delete operation is not invoked.

## Boundaries

- Authority evaluated on ContentItem X must not, by itself, permit deletion of unrelated ContentItem Y.
- Authority must respect the existing scope walk (`ContentItem → ContentType → Site → Global`); no scope layer is added or reordered.
- Permission lookup for this operation must never cross Site boundaries.
- Custos does not redefine the domain rule that delete requires `ARCHIVED`; the state-machine invariant is preserved.
- Retention, backup, and post-delete recoverability semantics are not defined here.
- Out of scope: role administration, direct actor grants, decision records, decision traces, metrics, persistence, collection-read authorization.

## Postconditions

### After an authorized call
- The ContentItem no longer exists in the domain per the pre-existing delete semantics.
- The same downstream effects that follow a normal delete occur.
- The observable outcome is indistinguishable from invoking delete through the unsecured runtime with a valid caller.

### After a denied call
- The ContentItem is unchanged and still exists.
- Its status is unchanged.
- No delete-related event, projection, or audit record exists for this attempt.
- The caller receives the standard Custos denial signal.

### After a precondition failure on a granted call
- The ContentItem is unchanged.
- Any resulting error is a domain precondition error, not a Custos denial.

### After an invariant violation
- No domain state changed.
- The fatal signal is not swallowed or downgraded.

## Observable Verifications

- A caller with sufficient authority can delete an archived ContentItem.
- A caller without sufficient authority cannot delete an archived ContentItem, and the item is intact afterward.
- A denied delete attempt produces none of the side effects that a successful delete would.
- Authority scoped to one ContentItem, ContentType, or Site does not enable delete on an unrelated resource.
- Authority granted at a broader scope may permit delete of items within that scope, per the existing scope walk — but not across Site boundaries.
- The secured runtime never falls back to an "unsupported operation" style refusal for this operation.

## Open Questions (Deferred to Design)

- Whether delete authority should be strictly stronger than update, archive, or publish authority given its destructive nature.
- Which built-in roles receive delete authority, if any.
- Whether delete authority is meaningful at broader scopes (ContentType, Site, Global) or should be constrained.
- Exact permission constants, method names, and identifiers.

These belong to design and are intentionally not answered here.
