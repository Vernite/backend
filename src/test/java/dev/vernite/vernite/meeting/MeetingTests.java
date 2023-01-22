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

package dev.vernite.vernite.meeting;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.utils.FieldErrorException;

class MeetingTests {

    private static final Project project = new Project("Name", "Description");
    private static Validator validator;

    @BeforeAll
    static void init() {
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void constructorBaseTest() {
        var meeting = new Meeting(project, "Name", "Description", new Date(1), new Date(1000));

        assertEquals("Name", meeting.getName());
        assertEquals("Description", meeting.getDescription());
        assertEquals(new Date(1), meeting.getStartDate());
        assertEquals(new Date(1000), meeting.getEndDate());

        meeting = new Meeting(project, "Name  ", "  Description", new Date(1), new Date(1000));

        assertEquals("Name", meeting.getName());
        assertEquals("Description", meeting.getDescription());
        assertEquals(new Date(1), meeting.getStartDate());
        assertEquals(new Date(1000), meeting.getEndDate());

        assertThrows(FieldErrorException.class, () -> new Meeting(project, "", "", new Date(1000), new Date(1)));
    }

    @Test
    void constructorCreateTest() {
        var meeting = new Meeting(project,
                new CreateMeeting("Name", "Description", null, new Date(1), new Date(1000), null));

        assertEquals("Name", meeting.getName());
        assertEquals("Description", meeting.getDescription());
        assertEquals(new Date(1), meeting.getStartDate());
        assertEquals(new Date(1000), meeting.getEndDate());
        assertNull(meeting.getLocation());
        assertTrue(meeting.getParticipants().isEmpty());

        meeting = new Meeting(project,
                new CreateMeeting("Name  ", "  Description", " Location", new Date(1), new Date(1000), null));

        assertEquals("Name", meeting.getName());
        assertEquals("Description", meeting.getDescription());
        assertEquals(new Date(1), meeting.getStartDate());
        assertEquals(new Date(1000), meeting.getEndDate());
        assertEquals("Location", meeting.getLocation());
    }

    @Test
    void updateTest() {
        var meeting = new Meeting(project, "Name", "Description", new Date(1), new Date(1000));
        meeting.update(new UpdateMeeting("NewName", "NewDescription", null, null, null, null));

        assertEquals("NewName", meeting.getName());
        assertEquals("NewDescription", meeting.getDescription());

        meeting.update(new UpdateMeeting("NewName  ", "  NewDescription", "NewLocation", new Date(1000), new Date(2000),
                null));

        assertEquals("NewName", meeting.getName());
        assertEquals("NewDescription", meeting.getDescription());
        assertEquals("NewLocation", meeting.getLocation());
        assertEquals(new Date(1000), meeting.getStartDate());
        assertEquals(new Date(2000), meeting.getEndDate());

        assertThrows(FieldErrorException.class,
                () -> meeting.update(new UpdateMeeting("", "", null, null, new Date(1), null)));
    }

    @Test
    void setNameTest() {
        var meeting = new Meeting(project, "Name", "Description", new Date(1), new Date(1000));
        meeting.setName("New name");

        assertEquals("New name", meeting.getName());

        meeting.setName("  New name  ");

        assertEquals("New name", meeting.getName());
    }

    @Test
    void setDescriptionTest() {
        var meeting = new Meeting(project, "Name", "Description", new Date(1), new Date(1000));
        meeting.setDescription("New description");

        assertEquals("New description", meeting.getDescription());

        meeting.setDescription("  New description  ");

        assertEquals("New description", meeting.getDescription());
    }

    @Test
    void validationValidTest() {
        assertTrue(validator.validate(new Meeting(project, "Name", "Description", new Date(1), new Date(1000))).isEmpty());
    }

    @Test
    void validationInvalidTest() {
        assertFalse(validator.validate(new Meeting(project, "", "Description", new Date(1), new Date(1000))).isEmpty());
        assertFalse(validator.validate(new Meeting(project, "  ", "Description", new Date(1), new Date(1000))).isEmpty());
        assertFalse(validator.validate(new Meeting(project, "a".repeat(51), "Description", new Date(1), new Date(1000))).isEmpty());
        assertFalse(validator.validate(new Meeting(project, "Name", "a".repeat(1001), new Date(1), new Date(1000))).isEmpty());
    }

}
