

# AGENTS.md

## Project: Codex

Codex is an experimental CMS engine written in Java. The goal is to design a clean, domain-driven core that can later be exposed through different runtimes such as Spring Boot, CLI tools, tests, or AI/agent integrations like Olórin.

The current priority is to keep the domain model clean, expressive, and independent from infrastructure frameworks.

## Architectural Principles

- Prefer a clean Java core over framework-first design.
- Use inversion of control from the beginning.
- Do not make the core depend directly on Spring Boot, JPA, HTTP, or external infrastructure.
- Keep Spring Boot, persistence, REST controllers, and external integrations as adapters or runtimes around the core.
- Favor explicit constructors and immutable objects when practical.
- Prefer records and value objects for small immutable domain concepts.
- Use interfaces for ports such as repositories, services, fetchers, validators, and external integrations.
- Avoid global service locators or hidden static dependencies.
- Prefer composition over inheritance.
- Keep code readable, boring, and testable.

## Java Guidelines

- Use modern Java, but avoid syntax that would unnecessarily reduce compatibility unless the project explicitly adopts it.
- Prefer constructor injection over field injection.
- Use `Objects.requireNonNull(...)` for required dependencies and constructor parameters.
- Keep comments and JavaDoc in English.
- Avoid over-engineering, but preserve extension points where the domain clearly needs them.
- Use meaningful domain names instead of generic technical names.
- Avoid leaking implementation details into public APIs.

## Package and Layering Guidelines

Suggested high-level direction:

```text
codex.cms.core
  domain objects
  value objects
  repository interfaces
  application services
  commands

codex.cms.runtime
  manual composition root
  in-memory wiring
  test/demo runtime

codex.cms.adapters
  persistence adapters
  REST adapters
  Spring Boot adapters
  external system adapters
```

The core should not import from adapter or runtime packages.

## Current Design Direction

The main domain concepts include:

- `Site`
- `SiteKey`
- `SiteAlias`
- `ContentType`
- `ContentItem`
- `Field`
- workflow concepts to be introduced later

A `SiteKey` should be treated as a stable lookup key for a site. It is not necessarily the canonical domain. Domains and aliases can change over time and should not be used as the only stable identity for code-level lookups.

## Repository and Service Direction

Start with in-memory repositories before adding real persistence.

Example direction:

```java
public interface SiteRepository {
    Site save(Site site);
    Optional<Site> findByKey(SiteKey key);
    boolean existsByKey(SiteKey key);
    List<Site> findAll();
}
```

Application services should receive dependencies through constructors:

```java
public final class SiteService {

    private final SiteRepository siteRepository;

    public SiteService(final SiteRepository siteRepository) {
        this.siteRepository = Objects.requireNonNull(siteRepository, "siteRepository cannot be null");
    }
}
```

## IoC Direction

Use IoC now, but do not require Spring Boot yet.

A small manual composition root is acceptable during early development:

```java
public final class CodexContext {
    // Wires repositories, services, and other components for tests or demos.
}
```

Later, Spring Boot can provide an external runtime/adapter using `@Configuration` and `@Bean` methods without polluting the core model.

## Testing Guidelines

- Add tests for domain invariants.
- Add tests for repository behavior.
- Add tests for application service rules.
- Prefer testing full use cases once the in-memory layer exists.
- Use test names that describe behavior clearly.

## Agent Instructions

When modifying this project:

1. Preserve the domain-first architecture.
2. Do not introduce Spring Boot annotations into core domain classes.
3. Do not add persistence frameworks until the in-memory model and services are validated.
4. Keep comments in English.
5. Prefer small, focused changes.
6. Update or add tests when changing behavior.
7. Avoid broad refactors unless explicitly requested.
8. Keep APIs expressive and aligned with CMS concepts.
9. Ask for clarification only when a decision would significantly affect the architecture.
10. When in doubt, favor a clean domain model over short-term framework convenience.

## Near-Term Roadmap

Recommended next steps:

1. Create repository interfaces for core entities.
2. Implement in-memory repositories.
3. Create application services around those repositories.
4. Build a manual `CodexContext` composition root.
5. Add use-case tests for creating and retrieving sites, content types, and content items.
6. Introduce workflow concepts after the basic content lifecycle is clear.
7. Add persistence and Spring Boot runtime only after the core behavior is stable.
