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

package dev.vernite.vernite.sprint;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Date;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.utils.FieldErrorException;

class SprintTests {

    private static Project project = new Project("Name", "Description");
    private static Validator validator;

    @BeforeAll
    static void init() {
        project.setId(1);

        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void constructorBaseTest() {
        var sprint = new Sprint("Name", new Date(1000), new Date(2000), Sprint.Status.ACTIVE, "Description", project);

        assertEquals("Name", sprint.getName());
        assertEquals(new Date(1000), sprint.getStartDate());
        assertEquals(new Date(2000), sprint.getFinishDate());
        assertEquals(Sprint.Status.ACTIVE.ordinal(), sprint.getStatus());
        assertEquals("Description", sprint.getDescription());
        assertEquals(project, sprint.getProject());

        sprint = new Sprint("  Name ", new Date(1000), new Date(2000), Sprint.Status.ACTIVE, " Description  ", project);

        assertEquals("Name", sprint.getName());
        assertEquals(new Date(1000), sprint.getStartDate());
        assertEquals(new Date(2000), sprint.getFinishDate());
        assertEquals(Sprint.Status.ACTIVE.ordinal(), sprint.getStatus());
        assertEquals("Description", sprint.getDescription());
        assertEquals(project, sprint.getProject());
    }

    @Test
    void constructorCreateTest() {
        var sprint = new Sprint(project, new CreateSprint("Name", "Description", new Date(1000), new Date(2000), 1));

        assertEquals("Name", sprint.getName());
        assertEquals(new Date(1000), sprint.getStartDate());
        assertEquals(new Date(2000), sprint.getFinishDate());
        assertEquals(1, sprint.getStatus());
        assertEquals("Description", sprint.getDescription());
        assertEquals(project, sprint.getProject());

        sprint = new Sprint(project,
                new CreateSprint("  Name  ", "  Description  ", new Date(1000), new Date(2000), 1));

        assertEquals("Name", sprint.getName());
        assertEquals(new Date(1000), sprint.getStartDate());
        assertEquals(new Date(2000), sprint.getFinishDate());
        assertEquals(1, sprint.getStatus());
        assertEquals("Description", sprint.getDescription());
        assertEquals(project, sprint.getProject());
    }

    @Test
    void updateTest() {
        var sprint = new Sprint("Name", new Date(1000), new Date(2000), Sprint.Status.ACTIVE, "Description", project);
        sprint.update(new UpdateSprint("New Name", "New Description", new Date(3000), new Date(4000), 2));

        assertEquals("New Name", sprint.getName());
        assertEquals(new Date(3000), sprint.getStartDate());
        assertEquals(new Date(4000), sprint.getFinishDate());
        assertEquals(2, sprint.getStatus());
        assertEquals("New Description", sprint.getDescription());
        assertEquals(project, sprint.getProject());

        sprint.update(new UpdateSprint("  New Name  ", "  New Description  ", new Date(3000), new Date(4000), 2));

        assertEquals("New Name", sprint.getName());
        assertEquals(new Date(3000), sprint.getStartDate());
        assertEquals(new Date(4000), sprint.getFinishDate());
        assertEquals(2, sprint.getStatus());
        assertEquals("New Description", sprint.getDescription());
        assertEquals(project, sprint.getProject());

        sprint.update(new UpdateSprint());

        assertEquals("New Name", sprint.getName());
        assertEquals(new Date(3000), sprint.getStartDate());
        assertEquals(new Date(4000), sprint.getFinishDate());
        assertEquals(2, sprint.getStatus());
        assertEquals("New Description", sprint.getDescription());
        assertEquals(project, sprint.getProject());

        var update = new UpdateSprint(null, "", new Date(4000), new Date(3000), 2);
        assertThrows(FieldErrorException.class, () -> sprint.update(update));
    }

    @Test
    void setNameTest() {
        var sprint = new Sprint("Name", new Date(1000), new Date(2000), Sprint.Status.ACTIVE, "Description", project);
        sprint.setName("New Name");

        assertEquals("New Name", sprint.getName());

        sprint.setName("  New Name  ");

        assertEquals("New Name", sprint.getName());
    }

    @Test
    void setDescriptionTest() {
        var sprint = new Sprint("Name", new Date(1000), new Date(2000), Sprint.Status.ACTIVE, "Description", project);
        sprint.setDescription("New Description");

        assertEquals("New Description", sprint.getDescription());

        sprint.setDescription("  New Description  ");

        assertEquals("New Description", sprint.getDescription());
    }

    @Test
    void validationValidTest() {
        assertTrue(validator.validate(new Sprint("Name", new Date(1000), new Date(), Sprint.Status.ACTIVE, "", project))
                .isEmpty());
    }

    @Test
    void validationInvalidTest() {
        var active = Sprint.Status.ACTIVE;
        assertFalse(validator.validate(new Sprint("", new Date(1000), new Date(), active, "", project)).isEmpty());
        assertFalse(validator.validate(new Sprint("a".repeat(51), new Date(1000), new Date(), active, "", project))
                .isEmpty());
        assertFalse(validator.validate(new Sprint(" ", new Date(1000), new Date(), active, "", project)).isEmpty());
        assertFalse(validator.validate(new Sprint("Name", new Date(1000), new Date(), active, "", null)).isEmpty());
    }

}
