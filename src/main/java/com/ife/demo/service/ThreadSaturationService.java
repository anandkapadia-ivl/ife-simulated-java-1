package com.ife.demo.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Creates named threads that remain parked for dump analysis.
 */
@Service
public class ThreadSaturationService {

    private final List<Thread> workers = new ArrayList<Thread>();

    public synchronized String createWaitingThreads(int count, long waitMs) {
        for (int i = 0; i < count; i++) {
            final CountDownLatch latch = new CountDownLatch(1);
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        latch.await(waitMs, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "IFE-WaitingWorker-" + (workers.size() + 1));
            t.setDaemon(true);
            workers.add(t);
            t.start();
        }
        return "Created " + count + " waiting worker threads";
    }
}
