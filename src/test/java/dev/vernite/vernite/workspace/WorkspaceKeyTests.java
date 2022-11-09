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

package dev.vernite.vernite.workspace;

import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import dev.vernite.vernite.user.User;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
class WorkspaceKeyTests {
    private static final User user = new User("name", "surname", "username", "email", "password", "English", "YYYY-MM-DD");

    @Test
    void equalsTest() {
        WorkspaceKey workspaceKey = new WorkspaceKey(1, user);
        WorkspaceKey workspaceKey2 = new WorkspaceKey(1, user);

        assertEquals(workspaceKey, workspaceKey2);
        assertNotEquals(new WorkspaceKey(2, user), workspaceKey);

        workspaceKey2.setUserId(999);
        assertNotEquals(workspaceKey, workspaceKey2);

        assertNotEquals(null, workspaceKey);
        assertNotEquals(new Object(), workspaceKey);
        assertEquals(false, workspaceKey.equals(null));
    }

    @Test
    void hashCodeTest() {
        WorkspaceKey workspaceKey = new WorkspaceKey(1, user);
        WorkspaceKey workspaceKey2 = new WorkspaceKey(1, user);

        assertEquals(workspaceKey.hashCode(), workspaceKey2.hashCode());

        workspaceKey2.setUserId(999);
        assertNotEquals(workspaceKey.hashCode(), workspaceKey2.hashCode());

        workspaceKey2.setUserId(user.getId());
        workspaceKey2.setId(2);

        assertNotEquals(workspaceKey.hashCode(), workspaceKey2.hashCode());

        assertEquals(workspaceKey.hashCode(), workspaceKey.hashCode());
    }

    @Test
    void compareToTest() {
        WorkspaceKey workspaceKey = new WorkspaceKey(1, user);
        WorkspaceKey workspaceKey2 = new WorkspaceKey(1, user);

        assertEquals(0, workspaceKey.compareTo(workspaceKey2));

        workspaceKey2.setUserId(999);
        assertNotEquals(0, workspaceKey.compareTo(workspaceKey2));
        assertEquals(true, workspaceKey.compareTo(workspaceKey2) < 0);

        workspaceKey2.setUserId(user.getId());
        workspaceKey2.setId(2);
        assertNotEquals(0, workspaceKey.compareTo(workspaceKey2));
        assertEquals(true, workspaceKey.compareTo(workspaceKey2) < 0);

    }
}
