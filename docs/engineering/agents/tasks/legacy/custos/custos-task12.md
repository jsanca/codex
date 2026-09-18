Task — Phase 1.2 Add or Verify AccessDecisionService Tests

Context:
Phase 1.1 confirmed that DefaultAccessDecisionService wiring is correct:

* delegates to PermissionResolver
* translates PermissionResolution into AccessDecision
* preserves ResourceRef
* uses ResourceScope explicitly for resolution
* does not hide ResourceRef/ResourceScope behind magic mapping
* propagates CustosAgentSuperAdminInvariantViolationException uncaught
* keeps Custos transport-agnostic

Now verify that the AccessDecisionService test suite fully protects this behavior.

Scope:
Focus only on AccessDecisionService tests and test-support cleanup if needed.

Likely file:

* codex-custos/src/test/java/.../DefaultAccessDecisionServiceTest.java

Production code should not change unless a genuine testability issue is found.

Do not introduce:

* new authorization semantics
* secured decorators
* domain-specific permission services
* direct actor PermissionGrant support
* REST
* persistence
* JWT/SAML/OIDC
* servlet APIs
* Spring Security
* Olorin/step-up behavior

Required test coverage to verify:

1. Granted resolution becomes AccessDecision.Granted.
2. Denied resolution becomes AccessDecision.Denied.
3. Actor is preserved on granted decision.
4. Actor is preserved on denied decision.
5. Permission is preserved on granted decision.
6. Permission is preserved on denied decision.
7. ResourceRef is preserved on granted decision.
8. ResourceRef is preserved on denied decision.
9. Reason is preserved from granted PermissionResolution.
10. Reason is preserved from denied PermissionResolution.
11. ResourceScope is passed to PermissionResolver through PermissionResolutionRequest.
12. ResourceRef is not used as resolver target.
13. CustosAgentSuperAdminInvariantViolationException propagates uncaught.
14. Invariant exception is not converted into AccessDecision.Denied.
15. Null resolver is rejected.
16. Null request is rejected.
17. Null snapshot is rejected.

Quality expectations:

* Tests should describe behavior, not implementation trivia.
* Use @Nested groups where helpful.
* Use @DisplayName with readable scenario names.
* Prefer AssertJ assertions.
* Prefer assertThatThrownBy for exceptions.
* Keep fixtures readable and domain-named.
* Avoid one-letter lambda or variable names in domain/security tests.
* Do not assert on DEBUG logs unless necessary.

If all required tests already exist:

* Do not change code unnecessarily.
* Report that Phase 1.2 was already satisfied.
* Include the list of existing tests that cover each requirement.

If small gaps exist:

* Add only the missing tests.
* Keep the service implementation unchanged unless strictly necessary.

Commands:

* mvn test -pl codex-custos -Dtest=DefaultAccessDecisionServiceTest
* mvn test -pl codex-custos

Expected output:

* Any test changes if needed.
* Full codex-custos suite green.
* Short report:

    * files changed
    * tests added or confirmed
    * coverage checklist
    * commands run
    * final test count
    * any uncertain areas
