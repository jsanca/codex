# Codex Engineering Log

This log records human-readable engineering milestones. It is not a roadmap, ADR,
task list, or review report.

Use it to preserve the timeline of meaningful project movement.

## 2026-08-05

### Codex OSK Alignment

Codex adopted an explicit OSK-style operating model for coordinating work,
reviews, documentation, research, and architectural memory.

The model is operational, not product architecture. Codex modules remain governed
by Codex's domain-driven/module-driven boundaries.

### Custos Phase 1.4 Secured Decorators Completed

Custos completed the secured service decorator slice for:

* `SecuredContentItemService`
* `SecuredContentTypeService`
* `SecuredSiteService`

The checkpoint is:

```text
docs/agents/checkpoints/CHECKPOINT-CODEX-CUSTOS-SECURED-DECORATORS.md
```

### Forwarding Services Not Used For Secured Decorators

Custos secured decorators intentionally do not use `Forwarding*Service`.

Reason: forwarding services delegate by default. Security decorators must make an
explicit decision for each method:

* authorize
* fail closed
* documented temporary pass-through

### Phase 3.1 Selected As Next Runtime Work

The next runtime work is Phase 3.1 Secured Runtime Composition.

The formal task is:

```text
docs/agents/tasks/runtime/Task-CODEX-016—SecuredRuntimeComposition.md
```

### Runtime Metadata Direction

Runtime composition should expose whether a runtime is `SECURED` or `UNSECURED`.

This is intended to make secured/unsecured composition explicit and inspectable
without renaming the existing unsecured `ConciliumRuntime.inMemory()` path.

## 2026-08-06

### CODEX-016 Secured Runtime Composition Completed

Phase 3.1 Secured Runtime Composition was completed.

`ConciliumRuntime.secured(...)` now exposes a secured runtime path that wraps Site,
ContentType, and ContentItem services with Custos authorization.

`ConciliumRuntime.inMemory()` remains available as an explicit unsecured path for
tests, low-level scenarios, and backward compatibility.

### CODEX-016.1 coreRuntime Bypass Warning Completed

The `ConciliumRuntime.coreRuntime()` Javadoc now documents the bypass risk on
secured runtimes.

Adapter, external, and domain entrypoints should use:

```text
runtime.siteService()
runtime.contentTypeService()
runtime.contentItemService()
```

They should not use raw core services from:

```text
runtime.coreRuntime()
```

for authorization-sensitive entrypoints.

### Runtime Security Metadata Finalized

`RuntimeSecurityMode.SECURED` and `RuntimeSecurityMode.UNSECURED` are available as
diagnostic metadata.

Security is enforced by the exposed service graph, not by the metadata value.

### Recommended Next Technical Task

Recommended next technical task:

```text
CODEX-017 Denied-operation side-effect consistency
```

This should expand consistency checks around denied operations and their effects
on mutation, events, cache, index, and Chronicon behavior.
