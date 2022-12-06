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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UpdateProjectTests {

    private static Validator validator;

    @BeforeAll
    static void init() {
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validationValidTest() {
        assertTrue(validator.validate(new UpdateProject()).isEmpty());
        assertTrue(validator.validate(new UpdateProject("Name", "Description", 1L)).isEmpty());
        assertTrue(validator.validate(new UpdateProject("  Name ", " ", 4L)).isEmpty());
        assertTrue(validator.validate(new UpdateProject("  Name ", " Description", 73L)).isEmpty());
        assertTrue(validator.validate(new UpdateProject("  Name ", " Description", null)).isEmpty());
        assertTrue(validator.validate(new UpdateProject("  Name ", null, 2L)).isEmpty());
        assertTrue(validator.validate(new UpdateProject(null, "Description", 3L)).isEmpty());
    }

    @Test
    void validationInvalidTest() {
        assertEquals(1, validator.validate(new UpdateProject("Name", "", -1L)).size());
        assertEquals(2, validator.validate(new UpdateProject("", null, 1L)).size());
        assertEquals(1, validator.validate(new UpdateProject("a".repeat(51), "", null)).size());
        assertEquals(1, validator.validate(new UpdateProject(null, "a".repeat(1001), 1L)).size());
    }

}
