# IFE Correlated Vulnerable Java Application

This project is intentionally vulnerable and is designed for Incident Forensic
Engine (IFE) artifact-correlation testing.

## Important correlation guarantee

Do NOT use the supplied original reference `heapdump.hprof` with this project.
This project generates a **matching heap dump (`heapdump-correlated.hprof`), thread dump (`ThreadDump-correlated.jstack`), and application log (`application-correlated.log`)** from this exact running codebase.

All generated artifacts contain application classes under:

    com.ife.demo.*

---

## Multithreading & Memory Defect Scenarios

The application reproduces realistic production failure patterns that correlate across thread dumps, heap dumps, and logs:

1. **Deadlock with Heap Retention (`DeadlockService`)**:
   - `IFE-DeadlockWorker-A` and `IFE-DeadlockWorker-B` enter a circular monitor dependency (`resourceA` <-> `resourceB`).
   - `jstack` reports `Found 1 deadlock.`
   - Each thread retains a heap-allocated `DeadlockPayload` byte buffer.

2. **ThreadLocal Memory Retention (`ThreadLocalLeakService`)**:
   - Pooled executor threads (`IFE-ThreadLocalWorker-1..8`) set `SESSION_CONTEXT` without invoking `remove()`.
   - In MAT heap analysis, `ThreadLocalMap` pins multi-megabyte `ThreadLocalSessionContext` instances.

3. **ThreadPool Queue Backlog Saturation (`AsyncQueueSaturationService`)**:
   - An unbounded `LinkedBlockingQueue` accumulates heavy task payloads (`HeavyQueuedTask`).
   - Worker threads (`IFE-AsyncQueueWorker-1..4`) are bottlenecked on a throttle latch.
   - In Heap Dump, `LinkedBlockingQueue$Node` objects dominate retained heap.

4. **Resource / DB Contention (`DatabaseContentionService`)**:
   - Concurrent HTTP requests compete for `dbGate` monitor lock while performing JDBC work.
   - Tomcat HTTP worker threads saturate and block waiting for monitor entry.

5. **Static Memory Retention (`MemoryLeakService`)**:
   - `/incident/memory-leak` appends byte arrays to static `RETAINED_REQUESTS`.

6. **Parked Worker Saturation (`ThreadSaturationService`)**:
   - `/incident/waiting-threads` creates 40+ `IFE-WaitingWorker-*` threads waiting on latches.

---

## Quick Start (Automated)

### 1. Build the application
```powershell
mvn clean package
```

### 2. Start the application
```powershell
java -jar target/ife-correlated-vulnerable-app-1.0.0.jar
```
*(Default port is 8088 to avoid port conflicts with local services on 8080)*

### 3. Generate Incident Workload
In another PowerShell window:
```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-incident.ps1 -Port 8088
```

### 4. Capture Correlated Dumps & Logs
Immediately run:
```powershell
powershell -ExecutionPolicy Bypass -File scripts/capture-dumps.ps1
```

All artifacts will be saved in `artifacts/`:
- `artifacts/ThreadDump-correlated.jstack`
- `artifacts/heapdump-correlated.hprof`
- `artifacts/application-correlated.log`

---

## Verifying the Correlated Signals

### In Thread Dump (`ThreadDump-correlated.jstack`)
```powershell
# Verify Deadlock
Select-String "deadlock" artifacts/ThreadDump-correlated.jstack

# Verify Contention & Workers
Select-String "IFE-" artifacts/ThreadDump-correlated.jstack
Select-String "DatabaseContentionService" artifacts/ThreadDump-correlated.jstack
```

### In Heap Dump (`heapdump-correlated.hprof`)
Open with Eclipse Memory Analyzer (MAT):
- Look for `com.ife.demo.service.MemoryLeakService` (`RETAINED_REQUESTS`)
- Look for `com.ife.demo.service.ThreadLocalLeakService` (`ThreadLocalMap`)
- Look for `com.ife.demo.service.AsyncQueueSaturationService` (`taskQueue` backlog)
- Look for `com.ife.demo.service.DeadlockService$DeadlockPayload`

### In Log File (`application-correlated.log`)
Contains timestamps, PID, endpoints invoked, and worker thread state transitions.
