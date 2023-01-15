/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2023, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
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

package dev.vernite.vernite.release;

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

class ReleaseTests {

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
        var release = new Release("Name", "Description", new Date(), project);

        assertEquals("Name", release.getName());
        assertEquals("Description", release.getDescription());
        assertEquals(project, release.getProject());
        assertEquals(0, release.getTasks().size());
        assertEquals(false, release.isReleased());

        release = new Release("  Name ", "  Description ", new Date(), project);

        assertEquals("Name", release.getName());
        assertEquals("Description", release.getDescription());
        assertEquals(project, release.getProject());
        assertEquals(0, release.getTasks().size());
        assertEquals(false, release.isReleased());
    }

    @Test
    void constructorCreateTest() {
        var release = new Release(project, new CreateRelease("Name", "Description", new Date()));

        assertEquals("Name", release.getName());
        assertEquals("Description", release.getDescription());
        assertEquals(project, release.getProject());
        assertEquals(0, release.getTasks().size());

        release = new Release(project, new CreateRelease("  Name ", "  Description ", new Date()));

        assertEquals("Name", release.getName());
        assertEquals("Description", release.getDescription());
        assertEquals(project, release.getProject());
        assertEquals(0, release.getTasks().size());
    }

    @Test
    void updateTest() {
        var release = new Release("Name", "Description", new Date(), project);

        release.update(new UpdateRelease("New Name", "New Description", new Date()));

        assertEquals("New Name", release.getName());
        assertEquals("New Description", release.getDescription());
        assertEquals(project, release.getProject());
        assertEquals(0, release.getTasks().size());

        release.update(new UpdateRelease("  New Name ", "  New Description ", new Date()));

        assertEquals("New Name", release.getName());
        assertEquals("New Description", release.getDescription());
        assertEquals(project, release.getProject());
        assertEquals(0, release.getTasks().size());

        release.update(new UpdateRelease());

        assertEquals("New Name", release.getName());
        assertEquals("New Description", release.getDescription());
        assertEquals(project, release.getProject());
        assertEquals(0, release.getTasks().size());
    }

    @Test
    void setNameTest() {
        var release = new Release("Name", "Description", new Date(), project);
        release.setName("New Name");

        assertEquals("New Name", release.getName());

        release.setName("   Name ");

        assertEquals("Name", release.getName());
    }

    @Test
    void setDescriptionTest() {
        var release = new Release("Name", "Description", new Date(), project);
        release.setDescription("New Description");

        assertEquals("New Description", release.getDescription());

        release.setDescription("   Description ");

        assertEquals("Description", release.getDescription());
    }

    @Test
    void validationValidTest() {
        assertTrue(validator.validate(new Release("Name", "Description", new Date(), project)).isEmpty());
        assertTrue(validator.validate(new Release("Name", "  ", new Date(), project)).isEmpty());
    }

    @Test
    void validationInvalidTest() {
        assertFalse(validator.validate(new Release("  ", "Description", new Date(), project)).isEmpty());
        assertFalse(validator.validate(new Release("Name", "Description", null, project)).isEmpty());
        assertFalse(validator.validate(new Release("a".repeat(51), "Description", new Date(), project)).isEmpty());
        assertFalse(validator.validate(new Release("Name", "a".repeat(1001), new Date(), project)).isEmpty());
    }

}
