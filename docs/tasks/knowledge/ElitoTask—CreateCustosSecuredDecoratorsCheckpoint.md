Elito Task — Create Custos secured decorators checkpoint

Context:
Codex has completed Custos Phase 1.4 secured service decorators.

Completed work:

* SecuredContentItemService
* SecuredContentTypeService
* SecuredSiteService
* SecuredContentItemServiceAuthorizationMatrixTest
* fail-closed behavior for unsupported sensitive operations
* Deep review confirmed Phase 1.4 PASS with zero blockers
* Codex has now been aligned with the OSK operating model

Goal:
Create a checkpoint document that preserves the architectural state, decisions, accepted gaps, and recommended next work after completing Custos secured decorators.

Scope:
Documentation only.

Create:

* docs/agents/checkpoints/CHECKPOINT-CODEX-CUSTOS-SECURED-DECORATORS.md

Do not modify:

* production code
* tests
* module-info.java

Checkpoint should include:

1. Summary

State that Custos Phase 1.4 secured service decorators are complete.

2. Completed components

List:

* SecuredContentItemService
* SecuredContentTypeService
* SecuredSiteService
* SecuredContentItemServiceAuthorizationMatrixTest
* Domain permission services
* AccessDecisionService integration

3. Authorization pattern

Document the repeated pattern:

* null guards
* permission service check
* AccessDecision.requireGranted()
* delegate call only after granted decision
* denied decisions do not reach delegate
* invariant exceptions propagate uncaught

4. Module boundary decision

Document:

* secured decorators live in codex-custos internal service
* codex-codex remains independent of codex-custos
* dependency direction remains: codex-custos -> codex-codex -> fundamentum

5. Forwarding service decision

Document:

* codex-codex has internal Forwarding*Service interfaces
* secured decorators intentionally do not use them
* reason: forwarding services delegate by default
* security decorators must decide each method explicitly:

    * authorize
    * fail closed
    * documented temporary pass-through

6. Fail-closed decisions

Document:

* ContentItem delete/restore fail closed
* Site unarchive fails closed
* no delete/restore/purge/unarchive permission vocabulary introduced prematurely

7. Documented pass-through gaps

Document:

* ContentItem findByContentType/findAll
* ContentType findBySiteKey/findAll
* Site findByAlias/findAll

Explain these are accepted temporary gaps pending:

* per-item filtering
* query-level filtering
* alias-to-SiteKey authorization strategy

8. Known future work

Include:

* secure runtime composition
* collection read filtering
* alias resolution before authorization
* archive/restore/purge semantics
* audit/authorization integration
* cache/index consistency tests under denied operations

9. Recommended next task

Recommend:

* Phase 3.1 Secured runtime composition

But note that before runtime composition, the team may optionally run a final docs sync review.

Expected output:

* created checkpoint file
* short summary
* any stale docs discovered
