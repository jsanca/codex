# Imaginarium Conceptual Examples

These examples use diagrams only. They are meant to explain conceptual flow, not
APIs or implementation.

## Example A: Summarize an Article

```mermaid
flowchart TD
    article["Article"]
    request["Summarize"]
    illuminarium["Illuminarium"]
    imaginarium["Imaginarium"]
    provider["Provider"]
    model["LLM"]
    summary["Summary"]

    article --> request
    request --> illuminarium
    illuminarium --> imaginarium
    imaginarium --> provider
    provider --> model
    model --> provider
    provider --> imaginarium
    imaginarium --> illuminarium
    illuminarium --> summary
```

## Example B: Create a Website

```mermaid
flowchart TD
    intent["Human intent: create a website"]
    platform["Future Agent Platform<br/>(provisional)"]
    planner["Planner"]
    workers["Workers<br/>(provisional)"]
    imaginarium["Imaginarium"]
    providers["Providers"]
    proposal["Proposed site plan"]
    codex["Codex domain services<br/>(through authorized capabilities)"]

    intent --> platform
    platform --> planner
    planner --> workers
    workers --> imaginarium
    imaginarium --> providers
    providers --> imaginarium
    imaginarium --> workers
    workers --> proposal
    proposal --> codex
```

## Example C: Observe a Mission

```mermaid
flowchart TD
    mission["Mission"]
    runtime["Runtime"]
    supervisor["Supervisor"]
    signal["Supervision signal"]
    application["Application reaction<br/>(explicit)"]

    mission --> runtime
    runtime --> supervisor
    supervisor --> signal
    signal --> application
```

## Example D: Enrichment Without Agents

```mermaid
flowchart TD
    content["Content item"]
    enrichment["Semantic enrichment"]
    illuminarium["Illuminarium"]
    imaginarium["Imaginarium"]
    provider["Provider"]
    projection["Enrichment projection"]

    content --> enrichment
    enrichment --> illuminarium
    illuminarium --> imaginarium
    imaginarium --> provider
    provider --> imaginarium
    imaginarium --> illuminarium
    illuminarium --> projection
```
