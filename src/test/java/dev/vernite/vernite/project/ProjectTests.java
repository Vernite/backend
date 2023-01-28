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

package dev.vernite.vernite.project;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.vernite.vernite.projectworkspace.ProjectWorkspace;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.workspace.Workspace;

class ProjectTests {

    private static final User user = new User("name", "surname", "username", "email", "password");
    private static Validator validator;

    @BeforeAll
    static void init() {
        user.setId(1);

        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void constructorBaseTest() {
        Project project = new Project("Name", "Description");

        assertEquals("Name", project.getName());
        assertEquals("Description", project.getDescription());
        assertNotNull(project.getTaskCounter());

        project = new Project("  Name ", "   Description  ");

        assertEquals("Name", project.getName());
        assertEquals("Description", project.getDescription());
        assertNotNull(project.getTaskCounter());
    }

    @Test
    void constructorCreateTest() {
        Project project = new Project(new CreateProject("Name", "Description", 1L));

        assertEquals("Name", project.getName());
        assertEquals("Description", project.getDescription());
        assertNotNull(project.getTaskCounter());

        project = new Project(new CreateProject("  Name ", "   Description  ", 1L));

        assertEquals("Name", project.getName());
        assertEquals("Description", project.getDescription());
        assertNotNull(project.getTaskCounter());
    }

    @Test
    void updateTest() {
        Project project = new Project("Name", "Description");
        project.update(new UpdateProject("NewName", "NewDescription", null));

        assertEquals("NewName", project.getName());
        assertEquals("NewDescription", project.getDescription());

        project.update(new UpdateProject("  Name ", "   Description  ", null));

        assertEquals("Name", project.getName());
        assertEquals("Description", project.getDescription());

        project.update(new UpdateProject());

        assertEquals("Name", project.getName());
        assertEquals("Description", project.getDescription());
    }

    @Test
    void isMemberTest() {
        Project project = new Project("Name", "Description");

        assertFalse(project.isMember(user));

        project.getUsers().add(user);
        project.getProjectWorkspaces().add(new ProjectWorkspace(project, new Workspace(1, "Name", user), 1L));

        assertTrue(project.isMember(user));
    }

    @Test
    void removeMemberTest() {
        Project project = new Project("Name", "Description");
        project.getUsers().add(user);
        project.getProjectWorkspaces().add(new ProjectWorkspace(project, new Workspace(1, "Name", user), 1L));

        assertNotNull(project.removeMember(user));

        assertNull(project.removeMember(user));
    }

    @Test
    void setNameTest() {
        Project project = new Project("Name", "Description");
        project.setName("New name");

        assertEquals("New name", project.getName());

        project.setName("  New name  ");

        assertEquals("New name", project.getName());
    }

    @Test
    void setDescriptionTest() {
        Project project = new Project("Name", "Description");
        project.setDescription("New description");

        assertEquals("New description", project.getDescription());

        project.setDescription("  New description  ");

        assertEquals("New description", project.getDescription());
    }

    @Test
    void validationValidTest() {
        assertTrue(validator.validate(new Project("Name", "Description")).isEmpty());
    }

    @Test
    void validationInvalidTest() {
        assertEquals(2, validator.validate(new Project("", "Description")).size());
        assertEquals(2, validator.validate(new Project("  ", "Description")).size());
        assertEquals(1, validator.validate(new Project("a".repeat(51), "Description")).size());
        assertEquals(1, validator.validate(new Project("Name", "a".repeat(1001))).size());
    }

    @Test
    void compareToTest() {
        Project project = new Project("name", "desc");
        Project other = new Project("other", "desc");

        assertEquals(true, project.compareTo(other) < 0);

        other.setName("name");
        other.setId(1);

        assertEquals(true, project.compareTo(other) < 0);

        other.setId(project.getId());

        assertEquals(0, project.compareTo(other));
    }
}
