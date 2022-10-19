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

package com.workflow.workflow.sprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import com.workflow.workflow.utils.FieldErrorException;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
public class SprintRequestTests {
    @Test
    void setNameTests() {
        SprintRequest sprintRequest = new SprintRequest();

        assertThrows(FieldErrorException.class, () -> sprintRequest.setName(null));
        assertThrows(FieldErrorException.class, () -> sprintRequest.setName("  "));
        String tooLongName = "a".repeat(51);
        assertThrows(FieldErrorException.class, () -> sprintRequest.setName(tooLongName));

        sprintRequest.setName("Test");
        assertEquals("Test", sprintRequest.getName().get());
    }

    @Test
    void setDescriptionTests() {
        SprintRequest sprintRequest = new SprintRequest();

        sprintRequest.setDescription("Test");
        assertEquals("Test", sprintRequest.getDescription().get());

        assertThrows(FieldErrorException.class, () -> sprintRequest.setDescription(null));

        sprintRequest.setDescription("  ");
        assertEquals("", sprintRequest.getDescription().get());
    }

    @Test
    void setStartDateTests() {
        SprintRequest sprintRequest = new SprintRequest();

        Date startDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
        sprintRequest.setStartDate(startDate);
        assertEquals(startDate, sprintRequest.getStartDate().get());
        assertThrows(FieldErrorException.class, () -> sprintRequest.setStartDate(null));

        SprintRequest sprintRequest2 = new SprintRequest();
        sprintRequest2.setFinishDate(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)));

        Date now = Date.from(Instant.now());
        assertThrows(FieldErrorException.class, () -> sprintRequest2.setStartDate(now));
    }

    @Test
    void setFinishDateTests() {
        SprintRequest sprintRequest = new SprintRequest();

        Date finishDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
        sprintRequest.setFinishDate(finishDate);
        assertEquals(finishDate, sprintRequest.getFinishDate().get());
        assertThrows(FieldErrorException.class, () -> sprintRequest.setFinishDate(null));

        SprintRequest sprintRequest2 = new SprintRequest();
        sprintRequest2.setStartDate(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));

        Date now = Date.from(Instant.now());
        assertThrows(FieldErrorException.class, () -> sprintRequest2.setFinishDate(now));
    }

    @Test
    void setStatusTests() {
        SprintRequest sprintRequest = new SprintRequest();

        sprintRequest.setStatus("Test");
        assertEquals("Test", sprintRequest.getStatus().get());

        assertThrows(FieldErrorException.class, () -> sprintRequest.setStatus(null));
    }
}
