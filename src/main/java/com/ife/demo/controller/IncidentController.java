package com.ife.demo.controller;

import com.ife.demo.service.AsyncQueueSaturationService;
import com.ife.demo.service.DatabaseContentionService;
import com.ife.demo.service.DeadlockService;
import com.ife.demo.service.MemoryLeakService;
import com.ife.demo.service.ThreadLocalLeakService;
import com.ife.demo.service.ThreadSaturationService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/incident")
public class IncidentController {

    private final DatabaseContentionService database;
    private final MemoryLeakService memory;
    private final ThreadSaturationService threads;
    private final DeadlockService deadlock;
    private final ThreadLocalLeakService threadLocalLeak;
    private final AsyncQueueSaturationService queueSaturation;

    public IncidentController(DatabaseContentionService database,
                              MemoryLeakService memory,
                              ThreadSaturationService threads,
                              DeadlockService deadlock,
                              ThreadLocalLeakService threadLocalLeak,
                              AsyncQueueSaturationService queueSaturation) {
        this.database = database;
        this.memory = memory;
        this.threads = threads;
        this.deadlock = deadlock;
        this.threadLocalLeak = threadLocalLeak;
        this.queueSaturation = queueSaturation;
    }

    @PostMapping("/db-contention")
    public String dbContention(@RequestParam(defaultValue = "15000") long sleepMs) {
        return database.saturate(sleepMs);
    }

    @PostMapping("/memory-leak")
    public String memoryLeak(@RequestParam(defaultValue = "5") int megabytes) {
        return memory.leak(megabytes);
    }

    @PostMapping("/waiting-threads")
    public String waitingThreads(@RequestParam(defaultValue = "25") int count,
                                 @RequestParam(defaultValue = "120000") long waitMs) {
        return threads.createWaitingThreads(count, waitMs);
    }

    @PostMapping("/deadlock")
    public String deadlock(@RequestParam(defaultValue = "5") int payloadMb) {
        return deadlock.triggerDeadlock(payloadMb);
    }

    @PostMapping("/threadlocal-leak")
    public String threadLocalLeak(@RequestParam(defaultValue = "10") int megabytes) {
        return threadLocalLeak.leak(megabytes);
    }

    @PostMapping("/queue-saturation")
    public String queueSaturation(@RequestParam(defaultValue = "10") int count,
                                  @RequestParam(defaultValue = "5") int megabytes) {
        return queueSaturation.enqueueHeavyTasks(count, megabytes);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<String, Object>();
        status.put("retainedMemoryObjects", memory.retainedObjectCount());
        status.put("queueBacklog", queueSaturation.getQueueBacklog());
        status.put("status", "UP");
        return status;
    }
}
