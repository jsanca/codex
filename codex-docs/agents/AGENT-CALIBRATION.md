# Clio Calibration Notes

## Preferences learned

- Prefer explicit value objects over raw strings.
- Prefer service decorators over direct infrastructure injection.
- Prefer Map<FieldKey, Field> over List<Field> for schema fields.
- Do not introduce dynamic runtime behavior unless explicitly requested.
- If a task repeats an existing pattern, preserve the pattern closely.
- Keep large entity builders vertically formatted in tests — one field per line, easier to read and review diffs.

## Corrections

- Avoid direct repository access from indexing subscribers; use projection sources (ContentItemProjectionSource).
- Avoid broad runtime builders unless the task asks for them.
- Do not include content values in events.
- Do not rely on unordered repository results in tests.
- If a method is idempotent, do not update updatedAt/updatedBy.
- When moving responsibility into a new abstraction, add focused tests for that new abstraction specifically (not just through higher-level tests). Example: RepositoryContentItemProjectionSource should have its own null-argument, successful-load, and missing-entity tests.
- Make sure new files are tracked/included before finishing a task (check git status).

## Decision Context

This task introduces a projection source because subscribers should not depend directly on repositories or lifecycle services.

Prefer:
- small projection read facades
- subscriber single responsibility
- future compatibility with cache/read-only unit of work

Avoid:
- adding cache now
- using ContentItemService as a god facade
- adding transaction management in this task

## Open Questions

- Should this exception live in api or internal?
- Should the subscriber use service or projection source?

Do not implement until these are resolved.

## Post-Task Report

After implementation, report:

- Files changed
- Tests run
- Whether tests passed
- Any intentional deviations from this task
- Any architectural questions discovered
- Any follow-up cleanup tasks you recommend

## Backlog Classification

Active:
- May be implemented when explicitly tasked.

Near-future:
- Be aware of it.
- Do not implement unless the task explicitly says so.

Future-forward:
- Document only.
- Do not add code.

## Current Classification

Active:
- Event subscriber routing
- Index projection source
- Runtime integration tests

Near-future:
- Cache foundation
- ContentSearchService
- IndexingPolicy

Future-forward:
- Speculum
- tenant-aware index ids
- dynamic subscriber registry
- PublishedPointer

## Task feedback:
- Accepted / Accepted with minor changes / Needs changes.
- Good:
    - ...
- Adjust next time:
    - ...
- Add to calibration:
    - ...
- Follow-up task:
    - ...
example:
Task feedback:
- Accepted with minor changes.
- Good: subscriber now uses ContentItemProjectionSource instead of repositories.
- Adjust next time: keep entity builders vertically formatted in tests.
- Add to calibration: projection subscribers should not access repositories directly.
- Follow-up task: add tests for RepositoryContentItemProjectionSource.

## Linter / formatting

- If code style suggests additional null validations, apply them if they preserve semantics.
- Do not let formatting changes expand into unrelated refactors.

## Concurrency in recording/test helpers

- When using `Collections.synchronizedList`, always synchronize explicitly on the list object when
  iterating or copying: `synchronized (list) { return List.copyOf(list); }`.
- Same for bulk clear in `clearRecording()`-style methods: `synchronized (list) { list.clear(); }`.
- Reason: individual `add()` calls are safe without explicit sync, but `List.copyOf()` iterates
  internally and is not protected by the wrapper's per-method lock.

## Exception handling conventions

Established after Task 23 retro session:

### Location
- Domain exceptions live in `codex.<module>.api.exception` (exported, part of the public contract).
- Callers must be able to catch specific domain exceptions — they cannot be hidden in `internal`.
- Export exception packages in module-info.java.

### Base class
- No common `CodexException` base class.
- Direct `RuntimeException` subclasses per domain exception.
- Each exception has a clear, specific name that describes the failure.

### Constructors
- Standard: `(String message)` + `(String message, Throwable cause)`.
- Optional domain-specific factory constructor: e.g., `SiteAlreadyExistsException(final SiteKey key)`.
- Do NOT use the four-argument Java legacy constructors (boolean suppression, writable stack trace).

### Projection layer failures
- `IllegalStateException` is correct for subscriber/projection failures (system invariant violation, not domain error).
- Example: "Content item not found for published event" — this is a system inconsistency, not a recoverable domain condition.
- Wrap in a specific exception only if callers need to retry or route differently.

### NotFoundException
- Keep generic `NotFoundException` in `codex.fundamentum.api.exception`.
- Add typed subclasses (`ContentItemNotFoundException`, `ContentTypeNotFoundException`) only when a caller needs to handle them differently.
- Start generic; promote to typed on demand.

## Backlog classification rule

- Active backlog: may be implemented in the next 1-3 tasks.
- Near-future: design-aware, do not implement unless task explicitly says so.
- Future-forward: document only; do not add code.
