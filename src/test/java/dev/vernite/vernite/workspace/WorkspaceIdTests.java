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

package dev.vernite.vernite.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;

class WorkspaceIdTests {

    @Test
    void compareToTest() {
        WorkspaceId firstId = new WorkspaceId(1, 1);
        WorkspaceId secondId = new WorkspaceId(1, 1);

        assertEquals(0, firstId.compareTo(secondId));
        assertEquals(0, secondId.compareTo(firstId));

        firstId.setId(4);

        assertEquals(1, firstId.compareTo(secondId));
        assertEquals(-1, secondId.compareTo(firstId));

        secondId.setUserId(3);

        assertEquals(-1, firstId.compareTo(secondId));
        assertEquals(1, secondId.compareTo(firstId));

        firstId.setId(1);

        assertEquals(-1, firstId.compareTo(secondId));
        assertEquals(1, secondId.compareTo(firstId));

        secondId.setId(2);

        assertEquals(-1, firstId.compareTo(secondId));
        assertEquals(1, secondId.compareTo(firstId));
    }

    @Test
    void validationTest() {
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        final Validator validator = factory.getValidator();
        // Test valid workspace id
        assertTrue(validator.validate(new WorkspaceId(1, 1)).isEmpty());
        assertTrue(validator.validate(new WorkspaceId(3, 7)).isEmpty());
        assertTrue(validator.validate(new WorkspaceId(0, 4)).isEmpty());
        // Test invalid workspace id
        assertEquals(1, validator.validate(new WorkspaceId(-1, 4)).size());
        assertEquals(1, validator.validate(new WorkspaceId(0, 0)).size());
        assertEquals(2, validator.validate(new WorkspaceId(-3, 0)).size());
    }

}
