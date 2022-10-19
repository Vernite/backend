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

package com.workflow.workflow.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
class ProjectRequestTests {
    @Test
    void setNameTests() {
        ProjectRequest projectRequest = new ProjectRequest();

        projectRequest.setName("Test");
        assertEquals("Test", projectRequest.getName().get());

        assertThrows(FieldErrorException.class, () -> projectRequest.setName(null));
        assertThrows(FieldErrorException.class, () -> projectRequest.setName("  "));

        String tooLong = "a".repeat(51);
        assertThrows(FieldErrorException.class, () -> projectRequest.setName(tooLong));
    }

    @Test
    void setWorkspaceIdTests() {
        ProjectRequest projectRequest = new ProjectRequest();

        projectRequest.setWorkspaceId(1L);
        assertEquals(1, projectRequest.getWorkspaceId().get());

        assertThrows(FieldErrorException.class, () -> projectRequest.setWorkspaceId(null));
    }

    @Test
    void createEntityTests() {
        ProjectRequest projectRequest = new ProjectRequest();

        assertThrows(FieldErrorException.class, () -> projectRequest.createEntity());

        ProjectRequest projectRequest2 = new ProjectRequest("Test", 1L);
        Project project = projectRequest2.createEntity();

        assertEquals("Test", project.getName());
    }
}
