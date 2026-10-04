package com.example.rms.shared.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Safe technical lifecycle evidence; contains no actor, content or credentials. */
public final class GraphTrace {
    private final long started = System.nanoTime();
    private final List<Map<String, Object>> events = new ArrayList<>();
    public long graphConnectionId;
    public Long jpaConnectionId;
    public boolean evicted;
    public Integer releaseResult;
    public Integer getLockResult;
    public long getLockElapsedNanos;

    public synchronized void event(String name) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("sequence", events.size() + 1);
        e.put("name", name);
        e.put("elapsedNanos", System.nanoTime() - started);
        events.add(e);
    }

    public synchronized List<Map<String, Object>> events() {
        return List.copyOf(events);
    }

    public Map<String, Object> evidence() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("graphConnectionId", graphConnectionId);
        values.put("jpaConnectionId", jpaConnectionId);
        values.put("releaseResult", releaseResult);
        values.put("getLockResult", getLockResult);
        values.put("getLockElapsedMs", getLockElapsedNanos / 1_000_000L);
        values.put("evicted", evicted);
        values.put("events", events());
        return values;
    }
}
