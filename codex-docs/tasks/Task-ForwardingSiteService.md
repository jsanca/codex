# Task: Implement ForwardingSiteService and refactor decorators

## Objective
Create `ForwardingSiteService` in `codex.codex.internal.service` — a default-method
interface that extends `SiteService` and forwards all method calls to a delegate
returned by the abstract method `getDelegate()`.

## ForwardingSiteService contract
- Lives in `codex.codex.internal.service` (internal, not exported)
- Extends `SiteService`
- Has one abstract method: `SiteService getDelegate()`
- All `SiteService` methods implemented as `default` forwarding to `getDelegate()`

## Refactor
Once `ForwardingSiteService` exists, refactor `EventPublishingSiteService` to:
- Implement `ForwardingSiteService` instead of `SiteService`
- Remove all forwarding boilerplate methods
- Keep only the methods it actually overrides with real behavior

## Constraints
- Follow CLAUDE.md conventions
- No Spring annotations
- Constructor injection
- Proper JavaDoc