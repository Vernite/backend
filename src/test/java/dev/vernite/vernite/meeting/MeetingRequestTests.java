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

 package dev.vernite.vernite.meeting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import dev.vernite.vernite.utils.FieldErrorException;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
public class MeetingRequestTests {
    @Test
    void setNameTests() {
        MeetingRequest meetingRequest = new MeetingRequest();

        meetingRequest.setName("Test");
        assertEquals("Test", meetingRequest.getName().get());

        assertThrows(FieldErrorException.class, () -> meetingRequest.setName(null));
        assertThrows(FieldErrorException.class, () -> meetingRequest.setName("  "));

        String tooLong = "a".repeat(51);
        assertThrows(FieldErrorException.class, () -> meetingRequest.setName(tooLong));
    }

    @Test
    void setDescriptionTests() {
        MeetingRequest meetingRequest = new MeetingRequest();

        meetingRequest.setDescription("Test");
        assertEquals("Test", meetingRequest.getDescription().get());

        assertThrows(FieldErrorException.class, () -> meetingRequest.setDescription(null));

        String tooLong = "a".repeat(1001);
        assertThrows(FieldErrorException.class, () -> meetingRequest.setDescription(tooLong));
    }

    @Test
    void setLocationTests() {
        MeetingRequest meetingRequest = new MeetingRequest();

        meetingRequest.setLocation("Test");
        assertEquals("Test", meetingRequest.getLocation().get());

        meetingRequest.setLocation(null);
        assertEquals(false, meetingRequest.getLocation().isPresent());

        String tooLong = "a".repeat(1001);
        assertThrows(FieldErrorException.class, () -> meetingRequest.setLocation(tooLong));
    }

    @Test
    void createEntityTests() {
        MeetingRequest meetingRequest = new MeetingRequest();
        meetingRequest.setName("Test");
        meetingRequest.setDescription("Test");
        meetingRequest.setStartDate(new Date());
        meetingRequest.setEndDate(Date.from(Instant.now().plusSeconds(100)));

        Meeting meeting = meetingRequest.createEntity(null);
        assertEquals("Test", meeting.getName());
        assertEquals("Test", meeting.getDescription());
        assertEquals(meetingRequest.getStartDate().get(), meeting.getStartDate());
        assertEquals(meetingRequest.getEndDate().get(), meeting.getEndDate());

        meetingRequest.setEndDate(Date.from(Instant.now().minusSeconds(100)));

        assertThrows(FieldErrorException.class, () -> meetingRequest.createEntity(null));
    }
}
