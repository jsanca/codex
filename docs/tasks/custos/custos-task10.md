Task J — Wire AccessDecisionService to PermissionResolver

Context:
PermissionResolver now exposes a semantic API:
PermissionResolution resolve(PermissionResolutionRequest request, PermissionResolutionSnapshot snapshot)

It returns an explainable PermissionResolution. The next step is to wire AccessDecisionService so callers can receive an AccessDecision.

Scope:
Implement AccessDecisionService wiring only.

Do not introduce:
- secured service decorators
- REST
- JWT
- SAML
- OIDC
- Spring Security
- servlet APIs
- persistence
- user management
- direct actor PermissionGrant support
- step-up approval
- Olorin plan execution

Design intent:
AccessDecisionService converts a PermissionResolution into an AccessDecision.

Keep responsibility split:
- PermissionResolver computes effective permissions.
- AccessDecisionService produces the authorization decision object used by callers.
- AccessDecision remains actor + permission + ResourceRef + reason.
- PermissionResolution remains actor + permission + ResourceScope + reason.

Important design question:
AccessDecision uses ResourceRef, while PermissionResolver currently uses ResourceScope.
Do not hide this difference.

Suggested approach:
1. Introduce a small request/context object if needed, instead of adding many parameters.
2. Keep ResourceRef-to-ResourceScope translation explicit.
3. If translation is not possible generically yet, start with a method that accepts both:
    - ResourceRef evaluatedResource
    - ResourceScope targetScope
      and document why.
4. Do not invent persistence or resource lookup.

Possible API:
AccessDecision evaluate(
Actor actor,
PermissionKey permission,
ResourceRef resource,
ResourceScope targetScope,
PermissionResolutionSnapshot snapshot
)

or, better if consistent with current service shape:
AccessDecision evaluate(
AccessDecisionRequest request,
PermissionResolutionSnapshot snapshot
)

where AccessDecisionRequest contains:
- Actor actor
- PermissionKey permission
- ResourceRef resource
- ResourceScope targetScope

Behavior:
- Build PermissionResolutionRequest(actor, permission, targetScope)
- Call PermissionResolver.resolve(...)
- If PermissionResolution.Granted → AccessDecision.granted(actor, permission, resource, resolution.reason())
- If PermissionResolution.Denied → AccessDecision.denied(actor, permission, resource, resolution.reason())
- Let CustosAgentSuperAdminInvariantViolationException propagate. Do not convert it to denied.

Tests:
- Granted resolution becomes AccessDecision.Granted.
- Denied resolution becomes AccessDecision.Denied.
- Reason is preserved.
- Actor and permission are preserved.
- ResourceRef is preserved.
- ResourceScope is used only for resolver targeting.
- Agent SUPER_ADMIN invariant exception propagates.
- No REST/security framework/persistence concerns.
- No direct actor PermissionGrant support.

Expected result:
- AccessDecisionService implementation, probably internal DefaultAccessDecisionService.
- Tests.
- Full reactor build green.
- Short report with files changed and design decisions.