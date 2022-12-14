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

package dev.vernite.vernite.common.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;

/**
 * Manager for states sent to integrated services. Its internal state is
 * thread-safe.
 */
@RequiredArgsConstructor
public class StateManager {

    private static record Item(long id, long expire) {

        /**
         * Checks if the item is invalid.
         * 
         * @param now the current time
         * @return true if the item is invalid
         */
        public boolean isInvalid(long now) {
            return expire < now;
        }

    }

    private final Map<String, Item> stateMap = new ConcurrentHashMap<>();

    private final long ttl;

    private final int stateSize;

    /**
     * Creates a new state manager with ttl of 5 minutes and state size of 128.
     */
    public StateManager() {
        this(1000 * 60 * 5, 128);
    }

    private void update() {
        var now = System.currentTimeMillis();
        stateMap.entrySet().removeIf(entry -> entry.getValue().isInvalid(now));
    }

    /**
     * Creates a new state.
     * 
     * @param id id connected to the state
     * @return the state
     */
    public String createState(long id) {
        update();
        var state = SecureRandomUtils.generateSecureRandomString(stateSize);
        stateMap.put(state, new Item(id, System.currentTimeMillis() + ttl));
        return state;
    }

    /**
     * Checks if the state is valid.
     * 
     * @param state the state
     * @return true if the state is valid
     */
    public boolean containsState(String state) {
        update();
        return stateMap.containsKey(state);
    }

    /**
     * Gets the id connected to the state. Removes the state from the manager in
     * case of success.
     * 
     * @param state the state
     * @return the id or null if the state is invalid
     */
    public Long retrieveState(String state) {
        update();
        var item = stateMap.get(state);

        if (item == null) {
            return null;
        }

        return item.id;
    }

}
