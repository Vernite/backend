/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2022, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package dev.vernite.vernite.integration.communicator.slack;

import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

public class StateManager {
    private record Container(long userId, Date time) {
    }

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
