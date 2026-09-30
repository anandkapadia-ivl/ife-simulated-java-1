package com.ife.demo.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Deliberately creates long-running DB work.
 *
 * Expected thread-dump signature:
 *   worker threads blocked/waiting around JDBC calls
 *   com.ife.demo.service.DatabaseContentionService.saturate()
 *
 * The synchronized gate makes the workload deterministic: only one worker
 * may hold the simulated DB resource while other request threads queue.
 */
@Service
public class DatabaseContentionService {

    private final JdbcTemplate jdbcTemplate;
    private final Object dbGate = new Object();

    public DatabaseContentionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String saturate(long sleepMs) {
        synchronized (dbGate) {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TEST_DATA", Integer.class);
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "DB operation completed after " + sleepMs + " ms";
        }
    }

    public String jdbcWait(long sleepMs) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            latch.await(sleepMs, TimeUnit.MILLISECONDS);
            return "JDBC-like wait completed";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Interrupted";
        }
    }
}
