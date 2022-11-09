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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.utils.FieldErrorException;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
class StatusRequestTests {
    @Test
    void setNameTests() {
        StatusRequest statusRequest = new StatusRequest();

        statusRequest.setName("Test");
        assertEquals("Test", statusRequest.getName().get());

        assertThrows(FieldErrorException.class, () -> statusRequest.setName(null));
        assertThrows(FieldErrorException.class, () -> statusRequest.setName("  "));

        String tooLong = "a".repeat(51);
        assertThrows(FieldErrorException.class, () -> statusRequest.setName(tooLong));
    }

    @Test
    void setColorTests() {
        StatusRequest statusRequest = new StatusRequest();

        statusRequest.setColor(0);
        assertEquals(0, statusRequest.getColor().get());

        assertThrows(FieldErrorException.class, () -> statusRequest.setColor(null));
    }

    @Test
    void setIsFinalTests() {
        StatusRequest statusRequest = new StatusRequest();

        statusRequest.setFinal(true);
        assertEquals(true, statusRequest.getFinal().get());

        assertThrows(FieldErrorException.class, () -> statusRequest.setFinal(null));
    }

    @Test
    void setIsBeginTests() {
        StatusRequest statusRequest = new StatusRequest();

        statusRequest.setBegin(true);
        assertEquals(true, statusRequest.getBegin().get());

        assertThrows(FieldErrorException.class, () -> statusRequest.setBegin(null));
    }

    @Test
    void setOrdinalTests() {
        StatusRequest statusRequest = new StatusRequest();

        statusRequest.setOrdinal(1);
        assertEquals(1, statusRequest.getOrdinal().get());

        assertThrows(FieldErrorException.class, () -> statusRequest.setOrdinal(null));
    }

    @Test
    void createEntityTests() {
        Project project = new Project();
        StatusRequest statusRequest = new StatusRequest();

        assertThrows(FieldErrorException.class, () -> statusRequest.createEntity(1, project));

        StatusRequest statusRequest2 = new StatusRequest("Test", 0, true, true, 1);
        Status status = statusRequest2.createEntity(1, project);

        assertEquals("Test", status.getName());
    }
}
