Improve shutdown semantics for BoundedVirtualCodexExecutor.

Context:
BoundedVirtualCodexExecutor uses:

- Executors.newVirtualThreadPerTaskExecutor()
- Semaphore to bound concurrent execution

The semaphore is intentionally acquired inside the virtual thread, so submit(Runnable) returns immediately. We want to preserve that behavior.

Concern:
shutdown() currently calls executor.shutdown(), which is graceful. It stops accepting new work but does not interrupt already accepted virtual threads. Some accepted virtual threads may be parked waiting on semaphore.acquire() until a permit is released.

Goal:
Make graceful vs immediate shutdown explicit and add a way to wait for termination.

Requirements:
1. Preserve current submit() behavior:
    - submit(Runnable) should continue to return immediately.
    - Do not move semaphore.acquire() to the caller thread.
    - Do not make submit() block waiting for a permit.

2. Keep shutdown() as graceful shutdown:
    - set closing = true
    - call executor.shutdown()
    - document that accepted tasks may continue running or waiting for permits.

3. Add shutdownNow() to CodexExecutor if it does not already exist.
   Suggested semantics:
    - set closing = true
    - call executor.shutdownNow()
    - interrupt virtual threads waiting on semaphore.acquire()
    - preserve permit release safety in finally

4. Add:
    - boolean awaitTermination(Duration timeout)

   Suggested semantics:
    - validate timeout is not null
    - delegate to ExecutorService.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)
    - restore interrupted status if interrupted
    - either return false or throw a RuntimeException on interruption, following existing project style

5. Update BoundedVirtualCodexExecutor accordingly.

6. Review RejectedExecutionException handling:
    - If rejection happens at executor.submit(...), handle it around the submit call.
    - Do not leave unreachable or misleading RejectedExecutionException handling inside the task body if it cannot actually happen there.

7. Preserve interruption behavior:
    - if a virtual thread is interrupted while waiting for a permit, restore interrupted status
    - do not leak semaphore permits
    - only release the semaphore if it was actually acquired

8. Add focused tests:
    - shutdown() rejects or ignores new submissions according to current CodexExecutor semantics.
    - awaitTermination(Duration) returns true after graceful completion.
    - shutdownNow() interrupts tasks waiting on semaphore.acquire().
    - running tasks release permits when they finish.
    - interrupted waiting tasks do not leak permits.
    - null timeout is rejected.
    - existing submit behavior remains non-blocking under saturation.

9. Do not add Observance metrics in this task.
10. Do not change CodexExecutorConfig unless strictly necessary.
11. Do not introduce external dependencies.
12. Do not replace the virtual-thread-per-task executor.

Expected result:
- BoundedVirtualCodexExecutor has clear graceful shutdown, immediate shutdown, and await termination behavior.
- submit() remains non-blocking with respect to semaphore permits.
- Virtual threads parked on semaphore.acquire() can be interrupted through shutdownNow().
- Tests cover the shutdown edge cases.