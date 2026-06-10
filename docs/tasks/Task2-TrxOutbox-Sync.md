# Task: Deferred Event Dispatcher and Transaction Callback System

## Problem
EventPublishingSiteService dispatches events before the transaction commits.
If the outer TransactionalSiteService rolls back, events are already fired.

## Solution

### TransactionContext (ScopedValue-based)
- Indicates whether the current execution is inside a transaction
- Uses ScopedValue, never ThreadLocal
- Lives in codex.fundamentum.api.tx

### TransactionCallback
- Interface with two methods: onCommit() and onRollback()
- Allows inner layers to register post-transaction actions
- Lives in codex.fundamentum.api.tx

### DeferredEventDispatcher implements CodexEventDispatcher
- If inside a transaction: accumulates events, registers a TransactionCallback
  that flushes on commit or discards on rollback
- If outside a transaction: dispatches immediately (read-only paths like findByKey)
- Lives in codex.codex.internal.service
- Wraps a real CodexEventDispatcher (the actual delivery mechanism)

## Constraints
- ScopedValue over ThreadLocal
- No Spring, no JTA
- Follow CLAUDE.md conventions