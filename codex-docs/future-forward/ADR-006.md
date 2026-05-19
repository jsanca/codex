# ADR-006: Logging Redaction and Security Logging Direction

## Status

Proposed

## Context

Codex currently uses SLF4J for technical logging and has separate infrastructure for audit and metrics:

- **Chronicon** records persistent business/domain audit records from domain events.
- **Observance** records local counters and timers for runtime behavior.
- **SLF4J logging** provides developer/operator-facing technical narrative.

As Codex grows, logs may accidentally receive sensitive data such as API keys, tokens, passwords, authorization headers, cookies, SAML assertions, JWTs, emails, account identifiers, or other PII. Developer discipline alone is not a sufficient protection mechanism. Codex should eventually provide a logging redaction safety net so accidental leaks are masked before logs leave the process.

This ADR captures the future direction only. It does not introduce production code yet.

---

## Decision

Codex should keep SLF4J as the logging API and eventually add a redaction layer at the logging backend/output boundary.

Codex should **not** introduce a custom `CodexLogger` wrapper prematurely.

The preferred future shape is:

```text
SLF4J Logger
  → Logback / Log4j2 appender, encoder, layout, or rewrite policy
      → Codex Redactor
          → console / file / log backend
```

This protects normal Codex logs and gives some protection for third-party/library logs that flow through the same backend.

---

## Rationale

### Keep SLF4J

SLF4J already provides the right abstraction for Java logging. Replacing it with a custom wrapper would force Codex to replicate or mediate features such as:

- placeholder formatting (`{}`)
- exception logging
- markers
- MDC
- log levels
- lazy evaluation patterns
- compatibility with third-party libraries

A custom wrapper would also not protect logs emitted directly by dependencies using SLF4J. A backend/output redaction layer is a better safety net.

### Redaction is a safety net, not permission to log secrets

Codex should still follow this rule:

```text
Never intentionally log raw secrets.
```

Redaction exists to reduce harm when sensitive data is accidentally included in a log message. It should not become an excuse to log arbitrary payloads.

---

## Boundaries

### Chronicon Audit

Chronicon records persistent business/domain history:

- who performed a domain operation
- what entity was affected
- when it happened
- what lifecycle action occurred

Chronicon is not a debug log and should not store transient technical messages.

### Observance

Observance records metrics:

- counters
- timers
- runtime behavior
- local node observations

Observance is not a logging system and should not store messages or sensitive payloads.

### Technical Logs

Technical logs provide developer/operator narrative:

- startup and shutdown
- runtime behavior
- errors and stack traces
- integration failures
- debugging context

Technical logs must be protected by redaction before leaving the process.

### Security Logs

Security logs are a future category for security-relevant runtime events:

- permission denied
- invalid actor context
- unauthorized mutation attempts
- invalid credentials or token validation failures
- suspicious repeated failures
- access attempts against protected resources

Security logs are distinct from Chronicon audit. Chronicon records successful domain lifecycle actions; security logs may include failed or suspicious attempts.

---

## Sensitive Data Classes

Future redaction should consider at least the following categories.

### Secrets

Secrets should generally be fully redacted:

- `password`
- `secret`
- `apiKey`
- `accessToken`
- `refreshToken`
- `Authorization` header
- cookies
- session ids
- private keys
- credentials

Example:

```text
apiKey=****
Authorization=****
password=****
```

### Authentication and Federation Payloads

These should generally be fully redacted or strongly summarized:

- JWTs
- SAML assertions
- OAuth tokens
- XML signatures
- private key material
- full certificates when not explicitly needed

### PII

PII may be masked or redacted depending on context:

- emails
- phone numbers
- external account ids
- user identifiers from external systems

Example:

```text
email=j***@example.com
```

---

## Future Implementation Direction

A future implementation may introduce a small redaction API, likely in `codex-fundamentum` or a logging-specific adapter module.

Possible API:

```java
public interface Redactor {
    String redact(String input);
}
```

Possible implementations:

```text
NoOpRedactor
DefaultSensitiveDataRedactor
PatternBasedRedactor
FieldNameBasedRedactor
```

Possible backend adapters:

```text
LogbackRedactingLayout
LogbackRedactingEncoder
Log4j2RedactingRewritePolicy
```

The exact backend should be decided later when Codex chooses its logging runtime configuration.

---

## Logging Backend Strategy

Codex should prefer backend/output redaction over application-level logger wrappers.

Possible backend integration points:

- Logback custom layout
- Logback encoder
- Logback converter
- Log4j2 rewrite policy
- Log4j2 custom plugin/converter

The implementation should be pluggable so Codex does not permanently couple core modules to one logging backend.

---

## Future Follow-ups

- Define a logging/security logging ADR with categories and logger naming conventions.
- Introduce a `Redactor` API.
- Implement a default sensitive-data redactor.
- Add Logback or Log4j2 integration.
- Add MDC/correlation id support.
- Add actor/site/request context to logs where appropriate.
- Define redaction tests for API keys, tokens, authorization headers, emails, JWTs, and SAML payloads.
- Define security log taxonomy.
- Add rules for avoiding sensitive data in exception messages.
- Define structured JSON logging direction.

---

## Non-goals

This ADR does not implement:

- a custom `CodexLogger`
- a redaction API
- a Logback or Log4j2 adapter
- structured logging
- security event taxonomy
- MDC/correlation id propagation
- changes to existing logging statements

---

## Summary

Codex should eventually include a logging redaction safety net, but it should not replace SLF4J or mix logging with Chronicon audit or Observance metrics.

The future direction is:

```text
Use SLF4J normally.
Redact at the logging backend/output boundary.
Never intentionally log secrets.
Treat redaction as defense-in-depth.
```
