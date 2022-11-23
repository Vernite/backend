package dev.vernite.vernite.integration.communicator.slack;

import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

public class StateManager {
    private record Container(long userId, Date time) {}

    private final HashMap<String, Container> inner = new HashMap<>();

    private void update() {
        for (Entry<String, Container> entry : inner.entrySet()) {
            if (new Date().getTime() - entry.getValue().time.getTime() > 300000) {
                inner.remove(entry.getKey());
            }
        }
    }

    public int size() {
        update();
        return inner.size();
    }

    
    public boolean isEmpty() {
        update();
        return inner.isEmpty();
    }

    
    public boolean containsKey(Object key) {
        update();
        return inner.containsKey(key);
    }

    
    public boolean containsValue(Object value) {
        update();
        return inner.containsValue(value);
    }

    
    public Long get(Object key) {
        update();
        return inner.get(key).userId;
    }

    
    public Long put(String key, Long value) {
        update();
        Container old = inner.put(key, new Container(value, new Date()));
        return old == null ? null : old.userId;
    }

    
    public Long remove(Object key) {
        return inner.remove(key).userId;
    }  
}
