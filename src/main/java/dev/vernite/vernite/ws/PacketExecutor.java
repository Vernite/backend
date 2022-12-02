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

package dev.vernite.vernite.ws;

import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;

import dev.vernite.protobuf.VerniteProtobuf;

public class PacketExecutor {
    private static final Logger L = Logger.getLogger("PacketExecutor");

    private static final Map<String, IHandler<? extends Message>> HANDLERS;
    private static final Map<String, Class<? extends Message>> PACKET_CLASSES;

    public static void call(SocketSession session, Any payload) throws InvalidProtocolBufferException {
        String type = getTypeNameFromTypeUrl(payload.getTypeUrl());
        @SuppressWarnings("unchecked")
        IHandler<Message> handler = (IHandler<Message>) HANDLERS.get(type);
        if (handler == null) {
            L.warning(session + ": No handler for " + type);
            return;
        }
        Message m = payload.unpack(PACKET_CLASSES.get(type));
        handler.handle(session, m);
    }

    private static String getTypeNameFromTypeUrl(String typeUrl) {
        int pos = typeUrl.lastIndexOf('/');
        if (pos == -1) {
            return "";
        }
        return typeUrl.substring(pos + 1);
    }

    private static void fill(HashMap<String, Class<? extends Message>> packetClasses,
            HashMap<String, IHandler<? extends Message>> handlers, Descriptor descriptor) {
        if (handlers.containsKey(descriptor.getFullName())) {
            throw new RuntimeException("Duplicate handler: " + descriptor.getFullName());
        }
        String pkg = SocketHandler.class.getPackageName();
        String name = descriptor.getName();
        String clName = String.format("%s.packets.%sHandler", pkg, name);
        try {
            Class<?> cl = SocketHandler.class.getClassLoader().loadClass(clName);
            for (var i : cl.getGenericInterfaces()) {
                if (!(i instanceof ParameterizedType)) {
                    continue;
                }
                ParameterizedType pt = (ParameterizedType) i;
                if (pt.getRawType() != IHandler.class) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Class<? extends Message> clazz = (Class<? extends Message>) pt.getActualTypeArguments()[0];
                packetClasses.put(descriptor.getFullName(), clazz);
            }
            IHandler<?> handler = (IHandler<?>) cl.getDeclaredConstructor().newInstance();
            handlers.put(descriptor.getFullName(), handler);

        } catch (ClassNotFoundException e) {
            // no handler
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    static {
        HashMap<String, Class<? extends Message>> packetClasses = new HashMap<>();
        HashMap<String, IHandler<? extends Message>> map = new HashMap<>();
        for (Descriptor m : VerniteProtobuf.getDescriptor().getMessageTypes()) {
            fill(packetClasses, map, m);
        }
        HANDLERS = Collections.unmodifiableMap(map);
        PACKET_CLASSES = Collections.unmodifiableMap(packetClasses);
    }
}
