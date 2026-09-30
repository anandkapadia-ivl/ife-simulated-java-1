package com.ife.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deliberate ThreadLocal memory retention defect within a pooled multi-threaded executor.
 *
 * In long-running thread pools, failure to call threadLocal.remove() causes each worker
 * thread's ThreadLocalMap to retain large objects indefinitely across task executions.
 */
@Service
public class ThreadLocalLeakService {
    private static final Logger log = LoggerFactory.getLogger(ThreadLocalLeakService.class);

    private static final ThreadLocal<ThreadLocalSessionContext> SESSION_CONTEXT = new ThreadLocal<ThreadLocalSessionContext>();

    private final ExecutorService workerPool = Executors.newFixedThreadPool(8, new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "IFE-ThreadLocalWorker-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    });

    public static class ThreadLocalSessionContext {
        private final String sessionId;
        private final byte[] buffer;

        public ThreadLocalSessionContext(String sessionId, int mb) {
            this.sessionId = sessionId;
            this.buffer = new byte[Math.max(1, mb) * 1024 * 1024];
        }

        public String getSessionId() {
            return sessionId;
        }

        public byte[] getBuffer() {
            return buffer;
        }
    }

    public String leak(final int megabytes) {
        for (int i = 0; i < 8; i++) {
            final int index = i;
            workerPool.submit(new Runnable() {
                @Override
                public void run() {
                    String sessionId = "SESSION-" + System.currentTimeMillis() + "-" + index;
                    // Deliberate defect: Storing heavy context in ThreadLocal without cleanup
                    SESSION_CONTEXT.set(new ThreadLocalSessionContext(sessionId, megabytes));
                    log.info("Assigned {} MB ThreadLocal payload to {}", megabytes, Thread.currentThread().getName());
                }
            });
        }
        return "Dispatched ThreadLocal leak across pool workers (" + megabytes + " MB each)";
    }
}
