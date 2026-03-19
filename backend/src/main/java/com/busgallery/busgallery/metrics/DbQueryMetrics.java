package com.busgallery.busgallery.metrics;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

@Component
public class DbQueryMetrics {

    private final LongAdder total = new LongAdder();
    private final LongAdder totalRead = new LongAdder();
    private final LongAdder totalWrite = new LongAdder();
    private final LongAdder slow = new LongAdder();
    private final LongAdder totalMs = new LongAdder();
    private final LongAccumulator maxMs = new LongAccumulator(Long::max, 0);

    private final Deque<SlowQuerySample> slowSamples = new ArrayDeque<>();
    private final int slowSampleLimit = 20;

    public void record(long elapsedMs, String statementId, String sql, boolean write, boolean isSlow) {
        total.increment();
        if (write) {
            totalWrite.increment();
        } else {
            totalRead.increment();
        }
        totalMs.add(elapsedMs);
        maxMs.accumulate(elapsedMs);
        if (isSlow) {
            slow.increment();
            addSlowSample(new SlowQuerySample(Instant.now().toString(), elapsedMs, statementId, sql));
        }
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> map = new HashMap<>();
        long totalCount = total.sum();
        map.put("total", totalCount);
        map.put("totalRead", totalRead.sum());
        map.put("totalWrite", totalWrite.sum());
        map.put("slow", slow.sum());
        map.put("avgMs", totalCount == 0 ? 0 : (double) totalMs.sum() / totalCount);
        map.put("maxMs", maxMs.get());
        map.put("slowSamples", slowSamples.toArray());
        return map;
    }

    private void addSlowSample(SlowQuerySample sample) {
        synchronized (slowSamples) {
            if (slowSamples.size() >= slowSampleLimit) {
                slowSamples.pollFirst();
            }
            slowSamples.addLast(sample);
        }
    }

    public record SlowQuerySample(String time, long elapsedMs, String statementId, String sql) {
    }
}
