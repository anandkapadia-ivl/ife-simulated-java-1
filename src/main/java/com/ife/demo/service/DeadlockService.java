package com.ife.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;

/**
 * Creates an intentional multi-threaded deadlock with retained heap payloads.
 *
 * Worker Thread A locks resourceA, then attempts to lock resourceB.
 * Worker Thread B locks resourceB, then attempts to lock resourceA.
 * Both threads hold on to heap-allocated DeadlockPayload objects,
 * demonstrating correlation between thread deadlock and pinned heap objects.
 */
@Service
public class DeadlockService {
    private static final Logger log = LoggerFactory.getLogger(DeadlockService.class);

    private final Object resourceA = new Object();
    private final Object resourceB = new Object();

    public static class DeadlockPayload {
        private final String threadName;
        private final byte[] payloadBuffer;

        public DeadlockPayload(String threadName, int mb) {
            this.threadName = threadName;
            this.payloadBuffer = new byte[Math.max(1, mb) * 1024 * 1024];
        }

        public String getThreadName() {
            return threadName;
        }

        public byte[] getPayloadBuffer() {
            return payloadBuffer;
        }
    }

    public synchronized String triggerDeadlock(final int payloadMb) {
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch lockAcquiredLatch = new CountDownLatch(2);

        Thread threadA = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startLatch.await();
                    synchronized (resourceA) {
                        log.info("DeadlockWorker-A acquired resourceA");
                        DeadlockPayload payloadA = new DeadlockPayload("IFE-DeadlockWorker-A", payloadMb);
                        lockAcquiredLatch.countDown();
                        Thread.sleep(100);
                        log.info("DeadlockWorker-A waiting for resourceB...");
                        synchronized (resourceB) {
                            log.info("DeadlockWorker-A acquired resourceB, payload size: {}", payloadA.getPayloadBuffer().length);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "IFE-DeadlockWorker-A");

        Thread threadB = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startLatch.await();
                    synchronized (resourceB) {
                        log.info("DeadlockWorker-B acquired resourceB");
                        DeadlockPayload payloadB = new DeadlockPayload("IFE-DeadlockWorker-B", payloadMb);
                        lockAcquiredLatch.countDown();
                        Thread.sleep(100);
                        log.info("DeadlockWorker-B waiting for resourceA...");
                        synchronized (resourceA) {
                            log.info("DeadlockWorker-B acquired resourceA, payload size: {}", payloadB.getPayloadBuffer().length);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "IFE-DeadlockWorker-B");

        threadA.setDaemon(true);
        threadB.setDaemon(true);
        threadA.start();
        threadB.start();
        startLatch.countDown();

        return "Deadlock triggered between IFE-DeadlockWorker-A and IFE-DeadlockWorker-B with " + payloadMb + "MB payloads";
    }
}
