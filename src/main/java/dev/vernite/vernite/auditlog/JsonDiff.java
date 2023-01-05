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

package dev.vernite.vernite.auditlog;

import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonDiff {

    public static ObjectMapper mapper = new ObjectMapper();

    /**
     * Compares two JsonNodes and returns three JsonNodes: oldValues, newValues and
     * sameValues. oldValues contains all fields that were removed or changed. newValues
     * contains all fields that were added or changed. sameValues contains all fields
     * that were not changed.
     * out[0] = oldValues
     * out[1] = newValues
     * out[2] = sameValues
     */
    public static void diff(JsonNode oldValue, JsonNode newValue, JsonNode[] out) {
        if (oldValue.getNodeType() != newValue.getNodeType()) {
            out[0] = oldValue;
            out[1] = newValue;
            out[2] = null;
            return;
        }
        switch (oldValue.getNodeType()) {
            case OBJECT:
                ObjectNode old = mapper.createObjectNode();
                ObjectNode neww = mapper.createObjectNode();
                ObjectNode same = mapper.createObjectNode();
                Iterator<String> it = oldValue.fieldNames();
                while (it.hasNext()) {
                    String field = it.next();
                    if (!newValue.has(field)) {
                        old.set(field, oldValue.get(field));
                    } else {
                        diff(oldValue.get(field), newValue.get(field), out);
                        if (out[0] != null) {
                            old.set(field, out[0]);
                        }
                        if (out[1] != null) {
                            neww.set(field, out[1]);
                        }
                        if (out[2] != null) {
                            same.set(field, out[2]);
                        }
                    }
                }
                it = newValue.fieldNames();
                while (it.hasNext()) {
                    String field = it.next();
                    if (!oldValue.has(field)) {
                        neww.set(field, newValue.get(field));
                    }
                }
                if (old.size() == 0) {
                    old = null;
                }
                if (neww.size() == 0) {
                    neww = null;
                }
                if (same.size() == 0) {
                    same = null;
                }
                out[0] = old;
                out[1] = neww;
                out[2] = same;
                break;
            case BINARY:
            case MISSING:
            case NULL:
            case POJO:
            case BOOLEAN:
            case ARRAY:
            case NUMBER:
            case STRING:
                if (oldValue.equals(newValue)) {
                    out[0] = null;
                    out[1] = null;
                    out[2] = oldValue;
                } else {
                    out[0] = oldValue;
                    out[1] = newValue;
                    out[2] = null;
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported node type: " + oldValue.getNodeType());
        }
    }
}
