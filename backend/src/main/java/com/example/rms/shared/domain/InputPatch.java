package com.example.rms.shared.domain;
import java.util.*;
/** Presence-aware command values, independent of HTTP/Jackson types. */
public final class InputPatch {
    private final Map<String,Object> fields;
    public InputPatch(Map<String,Object> fields,Set<String> allowed,String... controls) {
        if(fields==null || !allowed.containsAll(fields.keySet()))throw new RmsException(ErrorCode.INVALID_INPUT);
        this.fields=Collections.unmodifiableMap(new HashMap<>(fields));
        if(fields.keySet().stream().allMatch(Set.of(controls)::contains))throw new RmsException(ErrorCode.INVALID_INPUT);
    }
    public boolean has(String name) { return fields.containsKey(name); }
    public String string(String name,boolean nullable) {
        Object value=fields.get(name);if(value==null && nullable)return null;
        if(!(value instanceof String text))throw new RmsException(ErrorCode.INVALID_INPUT);return text;
    }
    public List<String> strings(String name) {
        Object raw=fields.get(name);if(!(raw instanceof List<?> list))throw new RmsException(ErrorCode.INVALID_INPUT);
        List<String> values=new ArrayList<>();for(Object value:list){if(!(value instanceof String text))throw new RmsException(ErrorCode.INVALID_INPUT);values.add(text);}
        if(new HashSet<>(values).size()!=values.size())throw new RmsException(ErrorCode.INVALID_INPUT);return List.copyOf(values);
    }
    @Override public String toString() { return "InputPatch[REDACTED]"; }
}
