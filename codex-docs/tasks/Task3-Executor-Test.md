# Task: Unit tests for DeferredEventDispatcher and TransactionContext

## Objective
Add unit test coverage for `DeferredEventDispatcher` and `TransactionContext`.
These components have non-trivial behavior that must be verified before
building more decorators on top.

## Test cases for DeferredEventDispatcher

### Outside a transaction
1. SYNC event dispatched immediately to delegate
2. ASYNC event dispatched immediately via asyncExecutor (not accumulated)

### Inside a transaction — commit path
3. SYNC event accumulated during transaction, dispatched to delegate on commit
4. ASYNC event accumulated during transaction, dispatched via asyncExecutor on commit
5. Multiple events accumulated in order, all dispatched on commit

### Inside a transaction — rollback path
6. SYNC event accumulated during transaction, discarded on rollback
7. ASYNC event accumulated during transaction, discarded on rollback
8. Multiple events accumulated, all discarded on rollback

## Test cases for TransactionContext

9. isActive() returns false outside a transaction
10. isActive() returns true inside runInTransaction()
11. Registered callback onCommit() is called after successful runInTransaction()
12. Registered callback onRollback() is called when runInTransaction() throws
13. Two nested or parallel transactions do not share the same context

## Constraints
- Follow CLAUDE.md conventions
- Test names must describe behavior clearly
- No Spring, no frameworks — plain JUnit 5
- Use a recording or mock dispatcher as delegate
- Keep tests focused and small