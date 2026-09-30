# Expected RCA / Validation Contract

## 1. Multi-Threaded Deadlock (Thread Dump + Heap Dump)
- **Defect**: Deterministic circular monitor dependency in `DeadlockService`.
- **Thread Dump Evidence**:
  - `jstack` explicitly flags `Found 1 deadlock.`
  - `IFE-DeadlockWorker-A` is waiting for monitor on `resourceB` held by `IFE-DeadlockWorker-B`.
  - `IFE-DeadlockWorker-B` is waiting for monitor on `resourceA` held by `IFE-DeadlockWorker-A`.
  - Stack traces highlight `com.ife.demo.service.DeadlockService$1.run` and `$2.run`.
- **Heap Dump Evidence**:
  - `DeadlockService$DeadlockPayload` pinned in memory by active worker stack frames.
  - Retained `byte[]` buffers associated with each deadlocked thread.

## 2. Multi-Threaded ThreadLocal Memory Retention (Thread Pool + Heap Dump)
- **Defect**: Failure to call `SESSION_CONTEXT.remove()` on reusable thread pool workers in `ThreadLocalLeakService`.
- **Thread Dump Evidence**:
  - Pool worker threads `IFE-ThreadLocalWorker-1` through `IFE-ThreadLocalWorker-8` alive in `WAITING` state.
- **Heap Dump Evidence**:
  - `java.lang.ThreadLocal$ThreadLocalMap` entries on `IFE-ThreadLocalWorker-*`.
  - Retained instances of `ThreadLocalLeakService$ThreadLocalSessionContext` holding multi-megabyte `byte[]` arrays.

## 3. Unbounded Queue Saturation with Throttled Consumers (Thread Dump + Heap Dump)
- **Defect**: `ThreadPoolExecutor` backed by unbounded `LinkedBlockingQueue` in `AsyncQueueSaturationService` where consumers are throttled on a countdown latch.
- **Thread Dump Evidence**:
  - Worker threads `IFE-AsyncQueueWorker-1` through `4` waiting on `throttleLatch.await()`.
- **Heap Dump Evidence**:
  - `AsyncQueueSaturationService$HeavyQueuedTask` backlog retained within `LinkedBlockingQueue$Node` instances.
  - Large accumulated payload buffers awaiting processing.

## 4. Database / Resource Contention (Thread Saturation)
- **Defect**: Monopolized synchronization on `DatabaseContentionService.dbGate` with simulated JDBC latency.
- **Thread Dump Evidence**:
  - HTTP request worker threads blocked waiting for `dbGate` monitor entry at `DatabaseContentionService.saturate()`.
  - Tomcat thread pool saturation (`server.tomcat.threads.max=20`).

## 5. Static Memory Retention (Heap Dump)
- **Defect**: Static accumulation list in `MemoryLeakService.RETAINED_REQUESTS`.
- **Heap Dump Evidence**:
  - Dominant memory consumer: `MemoryLeakService` holding tens to hundreds of megabytes in `RETAINED_REQUESTS` -> `byte[]`.

## 6. Thread Saturation via Parking
- **Defect**: Deliberately spawned worker threads in `ThreadSaturationService`.
- **Thread Dump Evidence**:
  - 40+ threads named `IFE-WaitingWorker-*` parked in `WAITING (parking)` on `CountDownLatch`.

## Correlation Requirement
The heap dump, thread dump, and application log MUST be generated from the same running JVM PID under active workload.
All signals correlate directly to `com.ife.demo.*` source code.
