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
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.vernite.vernite.user.User;

class WorkspaceTests {

    private static final User user = new User("name", "surname", "username", "email", "password", "English",
            "YYYY-MM-DD");
    private static Validator validator;

    @BeforeAll
    static void init() {
        user.setId(1);

        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void constructorBaseTest() {
        Workspace workspace = new Workspace(1, "Name", user);

        assertEquals(1, workspace.getId().getId());
        assertEquals("Name", workspace.getName());
        assertEquals(user, workspace.getUser());

        workspace = new Workspace(1, "  Name  ", user);

        assertEquals(1, workspace.getId().getId());
        assertEquals("Name", workspace.getName());
        assertEquals(user, workspace.getUser());
    }

    @Test
    void constructorCreateTest() {
        Workspace workspace = new Workspace(1, user, new CreateWorkspace("Name"));

        assertEquals(1, workspace.getId().getId());
        assertEquals("Name", workspace.getName());
        assertEquals(user, workspace.getUser());

        workspace = new Workspace(1, user, new CreateWorkspace("  Name  "));

        assertEquals(1, workspace.getId().getId());
        assertEquals("Name", workspace.getName());
        assertEquals(user, workspace.getUser());
    }

    @Test
    void updateTest() {
        Workspace workspace = new Workspace(1, "Name", user);
        workspace.update(new UpdateWorkspace("New name"));

        assertEquals(1, workspace.getId().getId());
        assertEquals("New name", workspace.getName());
        assertEquals(user, workspace.getUser());

        workspace.update(new UpdateWorkspace(null));

        assertEquals(1, workspace.getId().getId());
        assertEquals("New name", workspace.getName());
        assertEquals(user, workspace.getUser());
    }

    @Test
    void setNameTest() {
        Workspace workspace = new Workspace(1, "Name", user);
        workspace.setName("New name");

        assertEquals(1, workspace.getId().getId());
        assertEquals("New name", workspace.getName());
        assertEquals(user, workspace.getUser());

        workspace.setName("  New name  ");

        assertEquals(1, workspace.getId().getId());
        assertEquals("New name", workspace.getName());
        assertEquals(user, workspace.getUser());
    }

    @Test
    void validationValidTest() {
        assertTrue(validator.validate(new Workspace(1, "Name", user)).isEmpty());
    }

    @Test
    void validationInvalidTest() {
        assertEquals(2, validator.validate(new Workspace(1, "", user)).size());
        assertEquals(2, validator.validate(new Workspace(1, "  ", user)).size());
        assertEquals(3, validator.validate(new Workspace(-1, "   ", user)).size());
        assertEquals(1, validator.validate(new Workspace(1, "a".repeat(51), user)).size());
    }
}
