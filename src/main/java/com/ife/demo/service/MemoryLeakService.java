package com.ife.demo.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Deliberate memory-retention defect.
 *
 * Each call stores a large byte[] in a static list. The list is never cleared,
 * so retained objects are visible in a heap dump.
 */
@Service
public class MemoryLeakService {

    private static final List<byte[]> RETAINED_REQUESTS = new ArrayList<byte[]>();

    public synchronized String leak(int megabytes) {
        int bytes = Math.max(1, megabytes) * 1024 * 1024;
        RETAINED_REQUESTS.add(new byte[bytes]);
        return "Retained " + megabytes + " MB; total retained objects=" + RETAINED_REQUESTS.size();
    }

    public synchronized int retainedObjectCount() {
        return RETAINED_REQUESTS.size();
    }
}
