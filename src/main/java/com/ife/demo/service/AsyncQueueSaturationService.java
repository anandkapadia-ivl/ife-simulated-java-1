package com.ife.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deliberate unbounded queue saturation with thread bottleneck.
 *
 * Requests flood tasks into a ThreadPoolExecutor backed by an unbounded LinkedBlockingQueue.
 * The executor's worker threads are intentionally bottlenecked/throttled by a synchronization
 * gate or latch, causing tasks with large memory payloads to accumulate in the queue.
 *
 * In the Thread Dump: Workers appear WAITING/TIMED_WAITING on the choke point.
 * In the Heap Dump: LinkedBlockingQueue nodes hold huge retained memory from pending tasks.
 */
@Service
public class AsyncQueueSaturationService {
    private static final Logger log = LoggerFactory.getLogger(AsyncQueueSaturationService.class);

    private final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>();
    private final ThreadPoolExecutor executor;
    private final CountDownLatch throttleLatch = new CountDownLatch(1);

    public static class HeavyQueuedTask implements Runnable {
        private final String taskId;
        private final byte[] taskPayload;
        private final CountDownLatch latch;

        public HeavyQueuedTask(String taskId, int mb, CountDownLatch latch) {
            this.taskId = taskId;
            this.taskPayload = new byte[Math.max(1, mb) * 1024 * 1024];
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                // Workers wait on the latch simulating an upstream freeze / blocked consumer
                log.info("Task {} started on {}, waiting on throttle latch", taskId, Thread.currentThread().getName());
                latch.await(300, TimeUnit.SECONDS);
                log.info("Task {} finished", taskId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public String getTaskId() {
            return taskId;
        }

        public byte[] getTaskPayload() {
            return taskPayload;
        }
    }

    public AsyncQueueSaturationService() {
        this.executor = new ThreadPoolExecutor(
            4, 4, 60L, TimeUnit.SECONDS,
            taskQueue,
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "IFE-AsyncQueueWorker-" + counter.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            }
        );
    }

    public String enqueueHeavyTasks(int count, int megabytes) {
        for (int i = 0; i < count; i++) {
            HeavyQueuedTask task = new HeavyQueuedTask("TASK-" + (i + 1), megabytes, throttleLatch);
            executor.submit(task);
        }
        return "Enqueued " + count + " heavy tasks (" + megabytes + " MB each) into ThreadPool queue; backlog size=" + taskQueue.size();
    }

    public int getQueueBacklog() {
        return taskQueue.size();
    }
}
