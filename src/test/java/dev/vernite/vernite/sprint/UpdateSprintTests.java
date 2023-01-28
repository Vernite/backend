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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UpdateSprintTests {

    private static Validator validator;

    @BeforeAll
    static void init() {
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validationValidTest() {
        assertTrue(validator.validate(new UpdateSprint()).isEmpty());
        assertTrue(validator.validate(new UpdateSprint("Name", "desc", new Date(), new Date(), 1)).isEmpty());
        assertTrue(validator.validate(new UpdateSprint("Name", "", new Date(), new Date(), 1)).isEmpty());
        assertTrue(validator.validate(new UpdateSprint("Name", " desc", new Date(), new Date(), 1)).isEmpty());
        assertTrue(validator.validate(new UpdateSprint(" Name  ", " ", new Date(), new Date(), 1)).isEmpty());
        assertTrue(validator.validate(new UpdateSprint(null, "desc", new Date(), new Date(), 1)).isEmpty());
        assertTrue(validator.validate(new UpdateSprint("Name", null, new Date(), new Date(), 1)).isEmpty());
        assertTrue(validator.validate(new UpdateSprint("Name", "desc", null, new Date(), 1)).isEmpty());
        assertTrue(validator.validate(new UpdateSprint("Name", "desc", new Date(), null, 1)).isEmpty());
    }

    @Test
    void validationInvalidTest() {
        assertFalse(validator.validate(new UpdateSprint("", "desc", new Date(), new Date(), 1)).isEmpty());
        assertFalse(validator.validate(new UpdateSprint("  ", "desc", new Date(), new Date(), 1)).isEmpty());
        assertFalse(validator.validate(new UpdateSprint("a".repeat(51), "desc", new Date(), new Date(), 1)).isEmpty());
        assertFalse(validator.validate(new UpdateSprint("Name", "d".repeat(1001), new Date(), new Date(), 1)).isEmpty());
        assertFalse(validator.validate(new UpdateSprint("Name", "desc", new Date(), new Date(), -1)).isEmpty());
    }

}
