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

package dev.vernite.vernite.user.auth;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dev.vernite.vernite.common.utils.SecureRandomUtils;
import dev.vernite.vernite.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Component
public class VerificationEmails {
    private static final long CODE_MAX_TIME = TimeUnit.MINUTES.toMillis(30);
    private static final HashMap<String, VerificationEntry> code2user = new HashMap<>();
    private static final HashMap<String, VerificationEntry> email2user = new HashMap<>();

    /**
     * prepares the user to be registered
     * @param user the user to be registered
     * @return the code that should be sent to the user
     */
    public static synchronized String prepareUser(User user) {
        long t = System.currentTimeMillis();
        String code = SecureRandomUtils.generateSecureRandomString();
        VerificationEntry entry = email2user.get(user.getEmail().toLowerCase());
        if (entry != null && entry.destroyed) {
            entry = null;
        }
        if (entry != null) {
            entry.setTime(t);
            entry.setUser(user);
        } else {
            entry = new VerificationEntry(user, t, false);
        }
        code2user.put(code, entry);
        return code;
    }

    /**
     * returns the user that should be register
     * @param code the code that was in e-mail
     * @return user or null if code is invalid
     */
    public static synchronized User pollUser(String code) {
        VerificationEntry entry = code2user.remove(code);
        if (entry == null) {
            return null;
        }
        email2user.remove(entry.getUser().getEmail().toLowerCase());
        if (entry.isDestroyed()) {
            return null;
        }
        if (System.currentTimeMillis() - entry.getTime() > CODE_MAX_TIME) {
            entry.setDestroyed(true);
            return null;
        }
        entry.setDestroyed(true);
        return entry.getUser();
    }

    private static void clear(Iterator<VerificationEntry> it) {
        long now = System.currentTimeMillis();
        while (it.hasNext()) {
            VerificationEntry entry = it.next();
            if (entry.isDestroyed() || now - entry.getTime() > CODE_MAX_TIME) {
                it.remove();
            }
        }
    }

    private static synchronized void clear() {
        clear(code2user.values().iterator());
        clear(email2user.values().iterator());
    }

    @Scheduled(cron = "0 * * * * *")
    public void clearScheduler() {
        clear();
    }

    @Data
    @AllArgsConstructor
    private static class VerificationEntry {
        private User user;
        private long time;
        private boolean destroyed;
    }
}
