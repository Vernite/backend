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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
class ProjectTests {
    @Test
    void equalsTest() {
        Project project = new Project("name");
        Project other = new Project("other");

        assertEquals(false, project.equals(null));
        assertNotEquals(new Object(), project);
        assertNotEquals(other, project);

        other.setName("name");
        other.setId(1);

        assertNotEquals(other, project);

        other.setId(project.getId());
        assertEquals(other, project);

        project.setName(null);
        assertNotEquals(other, project);

        other.setName(null);
        assertEquals(other, project);

        project.setName("name");
        assertNotEquals(other, project);
    }

    @Test
    void hashCodeTest() {
        Project project = new Project("name");
        Project other = new Project("name");

        other.setId(1);
        assertNotEquals(other.hashCode(), project.hashCode());

        other.setId(project.getId());
        assertEquals(other.hashCode(), project.hashCode());
    }

    @Test
    void compareToTest() {
        Project project = new Project("name");
        Project other = new Project("other");

        assertEquals(true, project.compareTo(other) < 0);

        other.setName("name");
        other.setId(1);

        assertEquals(true, project.compareTo(other) < 0);

        other.setId(project.getId());

        assertEquals(0, project.compareTo(other));
    }
}
