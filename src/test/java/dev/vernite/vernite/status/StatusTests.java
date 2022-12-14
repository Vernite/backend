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

package dev.vernite.vernite.status;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.vernite.vernite.project.Project;

class StatusTests {

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
        Status status = new Status("Name", 0, 0, false, false, project);

        assertEquals("Name", status.getName());
        assertEquals(0, status.getColor());
        assertEquals(0, status.getOrdinal());
        assertFalse(status.isFinal());
        assertFalse(status.isBegin());
        assertEquals(project, status.getProject());

        status = new Status("  Name  ", 2, 1, false, true, project);

        assertEquals("Name", status.getName());
        assertEquals(1, status.getOrdinal());
        assertEquals(2, status.getColor());
        assertFalse(status.isFinal());
        assertTrue(status.isBegin());
        assertEquals(project, status.getProject());
    }

    @Test
    void constructorCreateTest() {
        Status status = new Status(project, new CreateStatus("Name", 0, 0, false, false));

        assertEquals("Name", status.getName());
        assertEquals(0, status.getColor());
        assertEquals(0, status.getOrdinal());
        assertFalse(status.isFinal());
        assertFalse(status.isBegin());
        assertEquals(project, status.getProject());

        status = new Status(project, new CreateStatus("  Name  ", 2, 1, false, true));

        assertEquals("Name", status.getName());
        assertEquals(1, status.getOrdinal());
        assertEquals(2, status.getColor());
        assertTrue(status.isFinal());
        assertFalse(status.isBegin());
    }

    @Test
    void updateTest() {
        Status status = new Status("Name", 0, 0, false, false, project);
        status.update(new UpdateStatus("New Name", 1, 2, true, true));

        assertEquals("New Name", status.getName());
        assertEquals(1, status.getColor());
        assertEquals(2, status.getOrdinal());
        assertTrue(status.isFinal());
        assertTrue(status.isBegin());

        status.update(new UpdateStatus("  New Name  ", 3, 4, false, false));

        assertEquals("New Name", status.getName());
        assertEquals(3, status.getColor());
        assertEquals(4, status.getOrdinal());
        assertFalse(status.isFinal());
        assertFalse(status.isBegin());

        status.update(new UpdateStatus());

        assertEquals("New Name", status.getName());
        assertEquals(3, status.getColor());
        assertEquals(4, status.getOrdinal());
        assertFalse(status.isFinal());
        assertFalse(status.isBegin());
    }

    @Test
    void setNameTest() {
        Status status = new Status("Name", 0, 0, false, false, project);
        status.setName("New Name");

        assertEquals("New Name", status.getName());

        status.setName("   Name  ");

        assertEquals("Name", status.getName());
    }

    @Test
    void validationValidTest() {
        assertTrue(validator.validate(new Status("Name", 0, 0, false, false, project)).isEmpty());
    }

    @Test
    void validationInvalidTest() {
        assertEquals(2, validator.validate(new Status("", 0, 0, false, false, project)).size());
        assertEquals(2, validator.validate(new Status("  ", 0, 0, false, false, project)).size());
        assertEquals(1, validator.validate((new Status("a".repeat(51), 0, 0, false, false, project))).size());
        assertEquals(1, validator.validate((new Status("Name", -1, 0, false, false, project))).size());
        assertEquals(1, validator.validate((new Status("Name", 1, -1, false, false, project))).size());
    }

}
