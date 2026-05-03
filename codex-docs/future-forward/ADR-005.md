# ADR-005: Tenant-Aware Index Document Identity

## Status

Future Forward

## Context

Codex is introducing a neutral indexing layer based on `IndexDocument`, `IndexDocumentId`, and `IndexWriter`.

For the current MVP, content item index document ids can be built using the local resource identity:

```text
content-item:{siteKey}:{contentTypeKey}:{contentItemKey}
```

This is sufficient while a Codex runtime owns its own isolated index.

However, future deployments may share the same indexing cluster across multiple customers, tenants, environments, or Codex runtimes to reduce infrastructure cost.

In that scenario, local resource identity may not be enough to guarantee index document uniqueness.

Decision

Codex will keep the current MVP index document id format simple.

For now, mappers may build ids such as:

content-item:{siteKey}:{contentTypeKey}:{contentItemKey}

Future multi-tenant or shared-cluster deployments should introduce an IndexDocumentIdFactory or equivalent strategy object.

That factory will be responsible for creating index document ids with the required deployment scope.

Possible future format:

tenant:{tenantKey}:content-item:{siteKey}:{contentTypeKey}:{contentItemKey}

or:

cluster:{clusterKey}:tenant:{tenantKey}:content-item:{siteKey}:{contentTypeKey}:{contentItemKey}

The exact format is intentionally deferred.

Consequences
Current indexing mappers remain simple.
Tenant concepts are not introduced prematurely.
Shared index clusters remain possible in the future.
Index document id construction can later be centralized without changing every mapper.
Backend-specific concerns such as OpenSearch index naming, aliases, routing, or shard strategy remain outside the domain model.
Future Work

Potential future components:

IndexDocumentIdFactory
TenantAwareIndexDocumentIdFactory
IndexNamespace
IndexScope
TenantKey
EnvironmentKey
ClusterKey

Future work should decide whether tenant/environment scope belongs in:

IndexDocumentId
IndexDocument metadata
IndexWriter configuration
Index alias/routing strategy

This ADR only records that the current simple id format is acceptable for the MVP, but should not be treated as the final multi-tenant indexing strategy.