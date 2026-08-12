Deep Review — Phase 3.1 Secured Runtime Composition Discovery

Context:
Custos Phase 1.4 secured service decorators are complete.

Completed:

* SecuredContentItemService
* SecuredContentTypeService
* SecuredSiteService
* domain permission services
* AccessDecisionService integration
* service-level authorization matrix test
* checkpoint and roadmap/checklist sync

Now we need to decide how secured services are composed into the runtime.

Goal:
Review the current Codex runtime/composition structure and identify the safest integration point for secured decorators.

This is review/discovery only.
Do not modify code.

Scope:
Review:

* Concilium/runtime composition code
* current service factory/composition classes
* module-info.java files
* tests around runtime composition
* docs/CODEX-ROADMAP-PHASES.md
* docs/agents/checkpoints/CHECKPOINT-CODEX-CUSTOS-SECURED-DECORATORS.md

Questions to answer:

1. Where are raw domain services currently created?
2. Is there already a composition root?
3. Does Concilium own service graph assembly?
4. Can codex-custos participate in composition without making codex-codex depend on codex-custos?
5. What is the safest way to wrap:

    * SiteService
    * ContentTypeService
    * ContentItemService
      with secured decorators?
6. Should secured runtime be the default?
7. Is an unsecured runtime still needed for tests/dev/internal scenarios?
8. How should PermissionResolutionSnapshot be supplied?
9. How should role assignments and built-in roles be supplied?
10. How can tests prove that the composed runtime exposes secured services, not raw delegates?

Evaluate possible approaches:

Option A:
Concilium directly composes raw services, then wraps them with Custos decorators.

Option B:
Custos exposes a small factory that wraps existing Codex services.

Option C:
A dedicated runtime module owns secure composition.

Option D:
Keep composition manual for now in tests only.

For each option, assess:

* dependency direction
* module boundary impact
* testability
* risk of bypassing secured decorators
* amount of code needed
* alignment with current roadmap

Output:

* PASS/WARN/BLOCKER summary
* current composition map
* recommended integration approach
* rejected approaches and why
* minimal next implementation task for Clio
* tests required to prove secure composition
