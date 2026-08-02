Task — Phase 1.1 Review AccessDecisionService Wiring

Context:
We are starting Codex Roadmap Phase 1: Finish Custos Core Authorization Loop.

The first checkpoint is to review and harden AccessDecisionService wiring.

Current intent:
AccessDecisionService should be a thin adapter between callers and PermissionResolver.

It should:

* receive an AccessDecisionRequest
* receive a PermissionResolutionSnapshot
* build a PermissionResolutionRequest using actor, permission, and targetScope
* delegate to PermissionResolver
* translate PermissionResolution.Granted / PermissionResolution.Denied into AccessDecision.Granted / AccessDecision.Denied
* preserve the original ResourceRef from AccessDecisionRequest
* preserve actor, permission, and reason
* allow CustosAgentSuperAdminInvariantViolationException to propagate uncaught

Scope:
Review and modify only what is needed for AccessDecisionService wiring and tests.

Focus files likely include:

* codex-custos/src/main/java/codex/custos/api/model/AccessDecisionRequest.java
* codex-custos/src/main/java/codex/custos/api/service/AccessDecisionService.java
* codex-custos/src/main/java/codex/custos/internal/service/DefaultAccessDecisionService.java
* codex-custos/src/test/java/.../DefaultAccessDecisionServiceTest.java

Do not introduce:

* REST
* persistence
* JWT
* SAML
* OIDC
* servlet APIs
* Spring Security
* secured domain decorators
* domain-specific permission services
* direct actor PermissionGrant support
* step-up approval
* Olorin execution

Review checklist:

1. Confirm AccessDecisionService delegates resolution to PermissionResolver.
2. Confirm PermissionResolution is translated into AccessDecision.
3. Confirm ResourceRef is preserved in AccessDecision.
4. Confirm ResourceScope is used explicitly for permission resolution.
5. Confirm ResourceRef and ResourceScope are not hidden behind magic mapping.
6. Confirm CustosAgentSuperAdminInvariantViolationException propagates uncaught.
7. Confirm no REST, persistence, JWT, SAML, OIDC, servlet, or Spring Security concerns leak into Custos.
8. Confirm logging does not expose excessive security detail.
9. Confirm null guards exist for request, snapshot, and injected resolver.
10. Confirm tests describe behavior, not implementation trivia.

Required tests:

* granted resolution becomes AccessDecision.Granted
* denied resolution becomes AccessDecision.Denied
* actor is preserved
* permission is preserved
* ResourceRef is preserved
* reason is preserved
* targetScope is passed to PermissionResolver
* ResourceRef is not used as the resolver target
* CustosAgentSuperAdminInvariantViolationException propagates uncaught
* null request is rejected
* null snapshot is rejected
* null resolver is rejected

Implementation guidance:

* Keep DefaultAccessDecisionService boring.
* Do not duplicate resolver logic.
* Do not inspect role assignments directly.
* Do not perform scope hierarchy walking here.
* Do not convert invariant violations into AccessDecision.Denied.
* Do not infer ResourceScope from ResourceRef.
* AccessDecisionService should remain transport-agnostic.

Expected output:

* Code changes only if needed.
* Tests added or strengthened if needed.
* Full relevant test suite green.
* Short report with:

    * files changed
    * test count / test command
    * confirmation of each checklist item
    * any uncertain design points
