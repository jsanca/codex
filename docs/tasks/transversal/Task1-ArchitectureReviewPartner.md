Review the current Observance architecture and identify structural inconsistencies.

Focus areas:

* metric naming consistency
* duplication across modules
* missing reusable abstractions
* potential cardinality risks
* decorator composition consistency
* constructor injection consistency
* opportunities for shared test utilities

Constraints:

* Avoid large production refactors
* Prefer reports and small safe improvements
* Do not modify files currently touched by Clio
* Respect ADR-008 and AGENTS.md conventions

Deliverables:

* short architecture review
* optional tiny cleanup commits
* list of future instrumentation candidates
