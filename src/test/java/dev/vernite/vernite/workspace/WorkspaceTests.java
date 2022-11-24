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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import dev.vernite.vernite.user.User;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
class WorkspaceTests {
    private static final User user = new User("name", "surname", "username", "email", "password", "English", "YYYY-MM-DD");

    @Test
    void equalsTest() {
        Workspace workspace = new Workspace(1, user, "Name");
        Workspace other = new Workspace(2, user, "Name 2");

        assertEquals(workspace, workspace);
        assertNotEquals(new Object(), workspace);
        assertNotEquals(null, workspace);
        assertEquals(false, workspace.equals(null));
        assertNotEquals(other, workspace);
        assertNotEquals(workspace, other);

        other.setUser(null);

        assertNotEquals(other, workspace);
        assertNotEquals(workspace, other);

        other.setId(null);

        assertNotEquals(other, workspace);
        assertNotEquals(workspace, other);

        other.setProjectWorkspaces(null);
        other.setName(null);

        assertEquals(other, other);
        assertNotEquals(other, workspace);
        assertNotEquals(workspace, other);

        Workspace other2 = new Workspace(3, user, "Name");

        WorkspaceId key = new WorkspaceId(1, user);
        key.setUserId(0);
        other2.setId(key);

        assertEquals(other2, workspace);

        other2.setId(null);
        other2.setUser(null);

        assertNotEquals(other, other2);
    }

    @Test
    void hashCodeTests() {
        Workspace workspace = new Workspace(1, user, "Name");
        Workspace other = new Workspace(2, user, "Name 2");

        assertEquals(workspace.hashCode(), workspace.hashCode());
        assertNotEquals(workspace.hashCode(), other.hashCode());

        other.setUser(null);

        assertNotEquals(workspace.hashCode(), other.hashCode());

        other.setId(null);

        assertNotEquals(workspace.hashCode(), other.hashCode());

        other.setProjectWorkspaces(null);
        other.setName(null);

        assertNotEquals(workspace.hashCode(), other.hashCode());

        Workspace other2 = new Workspace(3, user, "Name");

        WorkspaceId key = new WorkspaceId(1, user);
        key.setUserId(0);
        other2.setId(key);

        assertEquals(other2.hashCode(), workspace.hashCode());
    }

    @Test
    void compareToTests() {
        Workspace workspace = new Workspace(1, user, "Name");
        Workspace other = new Workspace(2, user, "Name");

        assertEquals(true, workspace.compareTo(other) < 0);

        other.setName("Name 2");

        assertEquals(true, workspace.compareTo(other) < 0);

        Workspace other2 = new Workspace(3, user, "Name");

        WorkspaceId key = new WorkspaceId(1, user);
        key.setUserId(0);
        other2.setId(key);

        assertEquals(0, workspace.compareTo(other2));

        assertEquals(0, workspace.compareTo(workspace));
    }

    @Test
    void updateTests() {
        Workspace workspace = new Workspace(1, user, "Name");
        workspace.update(new WorkspaceRequest());

        assertEquals(1, workspace.getId().getId());
        assertEquals(user, workspace.getUser());
        assertEquals("Name", workspace.getName());

        workspace.update(new WorkspaceRequest("Name 2"));

        assertEquals(1, workspace.getId().getId());
        assertEquals(user, workspace.getUser());
        assertEquals("Name 2", workspace.getName());
    }
}
