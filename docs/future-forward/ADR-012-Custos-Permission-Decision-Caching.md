# ADR-012: Custos Permission Decision Caching

## Status

Proposed / Future-forward

## Context

Custos authorization is moving toward an explicit flow:

```text
PermissionResolver -> PermissionResolution -> AccessDecisionService -> AccessDecision
```

`DefaultAccessDecisionService` should stay focused on translating a `PermissionResolution`
into an `AccessDecision`. It should not own cache state, invalidation, TTL policy, version
tracking, metrics, or logging for cache behavior.

Permission checks may eventually become high-volume, especially for read/list/view paths.
Caching can help, but authorization caching is security-sensitive: stale grants, stale
denials, role removal, and `SUPER_ADMIN` behavior must be handled conservatively.

## Decision

Permission-decision caching, if added, will be implemented as a decorator or upper layer
around `AccessDecisionService`.

Example future shape:

```text
CachingAccessDecisionService
  -> AccessDecisionService
     -> PermissionResolver
```

`DefaultAccessDecisionService` remains cache-free and pure.

## Cache Key

A cached decision is valid only for an explicit permission-state version.

The cache key must include:

```text
actor id
permission key
resource ref
target scope
permission-state version
```

The permission-state version represents the authorization data snapshot that made the
decision valid. If assignments, roles, grants, implication rules, or relevant permission
state change, the version changes and old decisions become unreachable.

## Version-Based Invalidation

Version-based invalidation is preferred over TTL-only caching.

TTL may exist as a safety net, but correctness must rely primarily on permission-state
versioning. A TTL-only cache can keep serving stale grants or stale denials after permission
state changes. A versioned key avoids that by making old entries irrelevant once the current
permission-state version advances.

Denied decisions may be cached only when tied to permission-state versioning. TTL-only denied
caching should be avoided because it can delay newly granted access.

## SUPER_ADMIN Decisions

`SUPER_ADMIN` bypass decisions must not be cached.

The cache layer should not perform a pre-check only to determine whether the actor is
`SUPER_ADMIN`. It should perform the normal cache lookup. On cache miss, it delegates to the
real `AccessDecisionService`. If the resulting decision was granted because of a
`SUPER_ADMIN` bypass, the cache layer returns the decision without storing it.

Reasons:

- `SUPER_ADMIN` is security-critical.
- The bypass is cheap to resolve.
- Cached super-admin decisions are riskier than useful.
- Hard invariants must run before any `SUPER_ADMIN` bypass.

## Role Removal

For v1, removing a role should increment the global permission-state version or otherwise
invalidate the full permission-decision cache.

Role removal can affect a complex graph of actors, role assignments, grants, scopes, inherited
permissions, and implication outcomes. Custos should favor security and consistency over cache
hit rate. More targeted invalidation can be designed later after the role/assignment graph is
better understood in production.

## Permission Sensitivity

Different permissions may use different cache policies.

Future abstractions may include `PermissionSensitivity` or `AccessDecisionCachePolicy`.
These would classify cache behavior without changing `DefaultAccessDecisionService`.

Initial policy guidance:

- Read/list/view permissions may use a longer TTL.
- Write/update/publish/delete permissions should use a shorter TTL or stricter policy.
- Permission-management and role-management operations should be non-cacheable or extremely
  short-lived.

These policies are secondary to permission-state versioning. TTL can constrain risk, but it
must not be the main correctness mechanism.

## Pseudo-Flow

```text
evaluate(actor, permission, resourceRef, targetScope, context):
    version = permissionStateVersion.current()
    key = AccessDecisionCacheKey(
        actor.id,
        permission,
        resourceRef,
        targetScope,
        version
    )

    cached = cache.get(key)
    if cached exists:
        record cache hit
        return cached

    record cache miss
    decision = delegate.evaluate(actor, permission, resourceRef, targetScope, context)

    if decision was granted by SUPER_ADMIN bypass:
        record bypass-not-cached
        return decision

    if cachePolicy.allowsCaching(permission, decision):
        cache.put(key, decision)

    return decision
```

The exact representation of "granted by SUPER_ADMIN bypass" is future work. It may be exposed
through structured decision metadata or an explanation trace. Until that exists, the cache
decorator must not infer super-admin status through an extra pre-check.

## Logging and Observability

Cache hit/miss logs should be debug-level.

Do not log sensitive actor internals. Actor id is acceptable if it is already considered safe
by the domain model.

Metrics are preferred for high-volume paths:

- cache hit rate
- cache miss rate
- eviction count
- bypass-not-cached count

## Consequences

### Positive

- Keeps `DefaultAccessDecisionService` simple and testable.
- Makes cache correctness depend on permission-state versioning, not time.
- Avoids storing high-risk `SUPER_ADMIN` bypass decisions.
- Provides a conservative v1 invalidation rule for role removal.
- Leaves room for permission-sensitive cache policy without forcing it into core decision logic.

### Tradeoffs

- Permission-state versioning must exist before this cache is correct.
- Broad invalidation on role removal reduces cache hit rate.
- Not caching `SUPER_ADMIN` bypass decisions sacrifices some performance for security clarity.
- Explanation metadata is needed before conditional storage can reliably detect
  `SUPER_ADMIN` bypass decisions.

## Non-Goals

- Do not add caching inside `DefaultAccessDecisionService`.
- Do not rely on TTL-only correctness.
- Do not cache `SUPER_ADMIN` bypass decisions.
- Do not design persistence, distributed cache topology, REST integration, JWT, SAML, OIDC,
  Spring Security, or servlet APIs.
- Do not implement direct actor `PermissionGrant` support as part of caching.

## Open Questions

- What component owns the permission-state version?
- Is the first version global-only, or should site-scoped versions exist later?
- How should `AccessDecision` expose that a decision was granted by `SUPER_ADMIN` bypass?
- Should cache policy be represented by `PermissionSensitivity`, `AccessDecisionCachePolicy`,
  or a smaller internal strategy?
