# Knowledge Routing and Progressive Retrieval Strategy

This document captures the current design hypothesis for Knowledge Routing in
Codex.

It is an architecture knowledge document, not a finalized implementation decision.
It does not define Java APIs, storage mechanisms, provider integrations, or a
roadmap.

## Problem Statement

Codex will need to retrieve knowledge from many heterogeneous sources:

* indexed content
* documentation
* structured records
* external APIs
* semantic projections
* future knowledge graphs
* controlled exploration environments

Treating every knowledge question as an open-ended reasoning problem would be too
expensive and too unpredictable. It can increase latency, token consumption,
provider cost, and unnecessary model inference.

The central challenge is to retrieve enough relevant context for a task while
avoiding unnecessary escalation. Exact identifiers, known paths, UUIDs, stack
traces, structured filters, and simple lexical lookups should not require broad
agentic exploration. At the same time, ambiguous or hybrid requests may need more
than one retrieval strategy.

Knowledge retrieval should therefore be treated as its own architectural concern,
separate from the reasoning process that consumes retrieved context.

## Architectural Principles

### Retrieval Is Independent From Reasoning

Retrieval decides where to search, how to search, and what context to assemble.

Reasoning consumes context and produces interpretation, explanation, plans, or
answers.

Keeping these responsibilities separate makes the system easier to optimize,
observe, test, and evolve.

### Prefer Deterministic Strategies Whenever Possible

When a request contains exact identifiers, direct references, UUIDs, stack traces,
known keys, or structured filters, deterministic retrieval should be attempted
before semantic or agentic approaches.

Deterministic retrieval is usually cheaper, faster, more explainable, and less
likely to introduce irrelevant context.

### Progressively Escalate Retrieval Cost

Retrieval should escalate only when the previous strategy cannot provide enough
confidence.

The system should prefer:

```text
simple retrieval
    -> combined retrieval
    -> guided exploration
    -> agentic exploration
```

This keeps expensive inference as a fallback rather than the default.

### Agents Operate On Knowledge Concepts, Not Files

Agents and reasoning processes should receive knowledge concepts and assembled
context, not raw storage primitives.

The retrieval layer may know how to map a question to files, records, vectors,
indexes, APIs, or future graph structures. The reasoning layer should not need to
own that mapping.

### Sandbox Exploration Is A Controlled Fallback

Sandbox exploration is useful when indexed retrieval is insufficient, but it should
not become unrestricted filesystem access.

It should expose knowledge-oriented operations and explicit budgets rather than
raw unbounded exploration.

### Context Assembly Optimizes Quality And Cost

Context assembly should balance relevance, completeness, latency, token cost, and
confidence.

More context is not always better. The goal is enough high-quality context for the
reasoning process to act responsibly.

## Knowledge Router

The Knowledge Router is the proposed routing concern that selects retrieval
strategies for a request.

It is a conceptual component. This document does not define an implementation
class or public API.

## Responsibilities

The Knowledge Router is responsible for:

* interpreting the shape of a knowledge request
* selecting one or more retrieval strategies
* ordering retrieval attempts from lower cost to higher cost
* preserving the separation between retrieval and reasoning
* producing routing decisions that can be inspected and improved
* allowing future routing policies to evolve without changing reasoning behavior

## Inputs

Conceptual inputs may include:

* the user request or system request
* known identifiers
* resource references
* query text
* structured filters
* task context
* policy context
* available retrieval providers
* cost or budget constraints

These inputs describe the retrieval problem. They are not themselves the final
reasoning context.

## Outputs

Conceptual outputs may include:

* selected retrieval strategy
* ordered retrieval strategy list
* routing rationale
* retrieval budgets
* requested confidence threshold
* constraints for guided exploration

The output should be usable by retrieval providers and context assembly without
coupling the router to a specific reasoning process.

## Decision Criteria

Version 1 should be a deterministic policy engine based on rules.

Example routing signals:

| Request shape | Preferred first strategy |
| --- | --- |
| Exact identifiers | Lexical or structured retrieval |
| UUIDs | Structured or exact lookup |
| Stack traces | Lexical retrieval with exact fragments |
| Natural language questions | Semantic or hybrid retrieval |
| Hybrid queries | Hybrid retrieval |
| Structured data requests | Structured retrieval |
| Low confidence after retrieval | Guided sandbox exploration |
| Still insufficient after guided exploration | Agentic exploration |

## Extensibility

Future versions may replace or complement deterministic rules with a specialized
lightweight routing model.

That model should preserve the same conceptual contract:

* receive a retrieval problem
* choose retrieval strategy or strategy sequence
* provide rationale or confidence
* stay independent from the downstream reasoning process

The router may become smarter, but retrieval remains distinct from reasoning.

## Retrieval Strategies

Each retrieval strategy has a different purpose and trade-off profile.

### Lexical Retrieval

Lexical retrieval uses exact or near-exact text matching such as BM25 or inverted
indexes.

Purpose:

* find exact names, identifiers, error messages, stack trace fragments, and known
  terminology

Strengths:

* fast
* explainable
* strong for exact terms
* low inference cost

Trade-offs:

* weak for paraphrase
* may miss conceptually related material with different wording

Typical use cases:

* stack traces
* method names
* file names
* event names
* domain identifiers

### Semantic Retrieval

Semantic retrieval uses vector similarity or related meaning-based search.

Purpose:

* find conceptually related material when exact wording is unknown

Strengths:

* handles paraphrase
* useful for exploratory questions
* can surface related concepts

Trade-offs:

* less deterministic
* can retrieve plausible but irrelevant context
* may require embeddings or model-dependent infrastructure

Typical use cases:

* natural language questions
* conceptual documentation lookup
* broad design exploration

### Hybrid Retrieval

Hybrid retrieval combines lexical and semantic retrieval.

Purpose:

* balance exactness and conceptual recall

Strengths:

* useful when queries mix exact tokens and broad intent
* can improve recall without discarding precision

Trade-offs:

* requires result merging and ranking
* can be harder to explain than single-strategy retrieval

Typical use cases:

* "find docs about ContentItem publish semantics"
* "where is this permission concept explained?"
* requests with both identifiers and natural language

### Structured Retrieval

Structured retrieval queries data through structured interfaces such as SQL-like
queries, typed APIs, catalogs, or metadata stores.

Purpose:

* answer questions that map to known fields, records, filters, or relationships

Strengths:

* precise
* efficient
* strong for known schemas
* easy to constrain

Trade-offs:

* requires structured sources
* weak for fuzzy or ambiguous intent

Typical use cases:

* lookup by UUID
* metadata filtering
* status checks
* catalog queries

### Knowledge Graph Retrieval

Knowledge graph retrieval is a future strategy based on explicit relationships
between concepts.

Purpose:

* traverse conceptual or domain relationships directly

Strengths:

* strong for connected reasoning
* can expose relationships that text search may miss
* useful for explainable concept navigation

Trade-offs:

* requires graph construction and maintenance
* relationship quality becomes critical

Typical use cases:

* concept dependency questions
* domain relationship exploration
* impact analysis across known concepts

### Guided Sandbox Exploration

Guided sandbox exploration allows controlled discovery when indexed retrieval is
insufficient.

Purpose:

* inspect knowledge sources through constrained operations

Strengths:

* useful when existing indexes are incomplete
* can follow local clues
* can discover missing context

Trade-offs:

* more expensive than direct retrieval
* needs budgets and constraints
* should not become unbounded browsing or filesystem access

Typical use cases:

* incomplete documentation indexes
* ambiguous references
* unfamiliar repository regions
* low-confidence retrieval results

## Progressive Retrieval Flow

The proposed retrieval lifecycle escalates only when needed.

```text
User Request
    ↓
Knowledge Router
    ↓
Selected Retrieval Strategy
    ↓
Context Confidence Evaluation
    ↓
If sufficient confidence:
    Return context
    ↓
Otherwise:
    Guided Sandbox Exploration
    ↓
If still insufficient:
    Agentic Exploration
```

The important behavior is not the exact sequence but the escalation discipline:
start with the cheapest likely-successful strategy, evaluate confidence, and only
then move to broader exploration.

## Context Confidence Evaluation

Context confidence evaluation asks whether retrieved material is good enough for
the downstream reasoning task.

Possible confidence signals include:

* exact identifier match
* source authority
* result agreement across strategies
* coverage of required concepts
* freshness requirements
* ambiguity remaining after retrieval
* missing referenced artifacts

Confidence evaluation should not require full reasoning when simple evidence is
enough.

## Guided Sandbox

The sandbox is a controlled discovery mechanism.

It is not unrestricted filesystem access.

It should expose a Knowledge API rather than storage primitives. A reasoning
process should request knowledge-oriented operations such as searching concepts,
opening candidate documents, following references, or inspecting bounded context.

Exploration should be constrained.

Possible budgets include:

* number of opened documents
* exploration depth
* token consumption
* execution time

The sandbox is not the primary retrieval strategy. It is a fallback when indexed,
semantic, hybrid, or structured retrieval cannot provide sufficient confidence.

## Separation Of Responsibilities

### Knowledge Router

The Knowledge Router selects retrieval strategy or strategy sequence.

It does not answer the user's question by itself and does not perform broad
reasoning over the final assembled context.

### Retrieval Providers

Retrieval Providers execute retrieval against specific source types.

They may represent lexical indexes, vector stores, structured APIs, future graph
stores, or guided sandbox operations.

They do not own reasoning behavior.

### Context Assembler

The Context Assembler turns retrieval results into usable context.

It balances relevance, quality, redundancy, ordering, token cost, and task
coverage.

It does not decide business semantics.

### Reasoning Process

The Reasoning Process consumes assembled context.

It may explain, summarize, plan, compare, or answer, depending on the task.

It should not need to know whether context came from lexical search, semantic
search, structured retrieval, or sandbox exploration.

## Future Evolution

Possible future enhancements include:

* learning-based routing
* adaptive retrieval policies
* retrieval metrics
* confidence calibration
* feedback-driven optimization
* automatic strategy selection
* retrieval-cost observability
* source-authority scoring
* query rewriting for retrieval only
* evaluation sets for routing quality

These ideas should preserve the core separation:

```text
retrieval finds and assembles context
reasoning uses context
```

## Current Status

This document captures a design proposal and current architectural direction.

It should guide future implementation and refinement, but it should not be treated
as proof that a Knowledge Router, retrieval provider layer, context assembler, or
sandbox interface already exists.
