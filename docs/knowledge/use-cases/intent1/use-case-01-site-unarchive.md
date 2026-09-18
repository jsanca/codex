# Use Case — Site Unarchive Under Secured Runtime

Status: Draft
Source of truth: `intent.md`
Scope: Custos C1 — Complete Secured Mutation Enforcement

## Summary

Establish the observable authorization behavior for the existing Site unarchive domain operation when it is invoked through the secured Codex runtime. The secured runtime must reach an explicit grant/deny decision instead of a fail-closed placeholder.

## Actors

- **Primary actor**: a caller (human or system) attempting to unarchive an archived Site.
- **Authorization system**: Custos, evaluating the caller's authority against the target Site.
- **Domain system**: the Codex core kernel that owns the Site lifecycle (`SUSPENDED ⟷ ARCHIVED`).
- **Excluded actor**: an `AgentActor` must never be a source of permission-management authority for this operation (existing Custos invariant).

## Preconditions

- The Site referenced by the request exists in the domain.
- The Site is currently in `ARCHIVED` status (unarchive is only meaningful from `ARCHIVED`).
- The caller is a well-formed Actor recognizable by Custos.
- A permission resolution snapshot exists for the current evaluation.
- The runtime under exercise is the secured composition, not the in-memory unsecured composition.
- No parallel or ad-hoc authorization mechanism is introduced to service this operation.

## Trigger

The caller invokes the Site unarchive operation exposed by the secured runtime for a specific Site.

## Main Flow — Authorized

1. The secured boundary receives the unarchive request for the target Site.
2. Custos evaluates the caller's authority for an unarchive-shaped permission at a resource scope that includes the target Site.
3. Custos returns a granted decision.
4. The domain unarchive operation is invoked with the original arguments.
5. The Site transitions from `ARCHIVED` to `SUSPENDED` (per the existing state machine).
6. Whatever domain events, projections, or audit records that would normally result from a successful unarchive occur exactly as they would outside the secured runtime.

## Alternate Flow — Unauthorized

1. The secured boundary receives the unarchive request for the target Site.
2. Custos evaluates the caller's authority and returns a denied decision.
3. The secured boundary raises the standard Custos denial signal used by every other secured mutation.
4. The domain unarchive operation is not invoked.
5. No domain event, projection update, or Chronicon audit record attributable to this denied attempt is produced.
6. The Site remains in `ARCHIVED` status.

## Alternate Flow — Hard Invariant Violation

1. The evaluation surfaces a Custos hard invariant violation (for example, an `AgentActor` improperly holding administrative authority).
2. The violation propagates as a fatal Custos error and is not silently converted into a denial.
3. The domain unarchive operation is not invoked.

## Boundaries

- Authority evaluated on Site A must not, by itself, permit unarchiving Site B. Cross-Site leakage is out of bounds.
- The scope walk must respect the existing bottom-up resolution (`Site → Global`); no new scope layer is introduced.
- Custos governs *whether* unarchive may execute; it does not redefine what unarchive means, including the target status (`SUSPENDED`).
- This use case does not cover role assignment, permission granting, direct actor grants, decision auditing, metrics, or persistence.
- This use case does not alter the pre-existing authorization behavior of any other Site operation.

## Postconditions

### After an authorized call
- The Site is `SUSPENDED`.
- The same downstream effects that follow a normal unarchive occur (events, projections, audit).
- The observable behavior is indistinguishable from invoking unarchive through the unsecured runtime with a valid caller.

### After a denied call
- The Site status is unchanged (`ARCHIVED`).
- No unarchive-related event, projection, or audit record exists for this attempt.
- The caller receives the standard Custos denial signal.

### After an invariant violation
- No domain state changed.
- The fatal signal is not swallowed or downgraded.

## Observable Verifications

The following properties must be observable end-to-end from a caller's perspective, without prescribing a specific verification mechanism:

- A caller with sufficient authority can complete unarchive on an archived Site.
- A caller without sufficient authority cannot complete unarchive, and the Site is unchanged afterward.
- A denied unarchive attempt produces none of the side effects that a successful unarchive would.
- Authority scoped to one Site does not enable unarchive on a different Site.
- The secured runtime never falls back to an "unsupported operation" style refusal for this operation.

## Open Questions (Deferred to Design)

- Which built-in roles receive authority for this operation, if any.
- Whether unarchive authority should be stronger, weaker, or equal to existing lifecycle authority (archive, suspend, start).
- Whether the authority permits unarchive only at Site scope or also at broader scopes.
- Exact permission constants, method names, and identifiers.

These belong to design and are intentionally not answered here.
