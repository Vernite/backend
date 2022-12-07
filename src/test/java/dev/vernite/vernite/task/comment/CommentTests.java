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

package dev.vernite.vernite.task.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.vernite.vernite.task.Task;
import dev.vernite.vernite.user.User;

class CommentTests {

    private static final User user = new User("name", "surname", "username", "email", "password");
    private static final Task task = new Task(1, "Name", "Description", null, user, 0, "low");
    private static Validator validator;

    @BeforeAll
    static void init() {
        user.setId(1);

        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void constructorBaseTest() {
        Comment comment = new Comment(task, "Content", user);

        assertEquals("Content", comment.getContent());
        assertEquals(user, comment.getUser());
        assertEquals(task, comment.getTask());
        assertNotNull(comment.getCreatedAt());

        comment = new Comment(task, "  Content  ", user);

        assertEquals("Content", comment.getContent());
        assertEquals(user, comment.getUser());
        assertEquals(task, comment.getTask());
        assertNotNull(comment.getCreatedAt());
    }

    @Test
    void constructorCreateTest() {
        Comment comment = new Comment(task, user, new CreateComment("Content"));

        assertEquals("Content", comment.getContent());
        assertEquals(user, comment.getUser());
        assertEquals(task, comment.getTask());
        assertNotNull(comment.getCreatedAt());

        comment = new Comment(task, user, new CreateComment("  Content  "));

        assertEquals("Content", comment.getContent());
        assertEquals(user, comment.getUser());
        assertEquals(task, comment.getTask());
        assertNotNull(comment.getCreatedAt());
    }

    @Test
    void updateTest() {
        Comment comment = new Comment(task, "Content", user);
        comment.update(new UpdateComment("New content"));

        assertEquals("New content", comment.getContent());
        assertEquals(user, comment.getUser());
        assertEquals(task, comment.getTask());
        assertNotNull(comment.getCreatedAt());

        comment.update(new UpdateComment(null));

        assertEquals("New content", comment.getContent());
        assertEquals(user, comment.getUser());
        assertEquals(task, comment.getTask());
        assertNotNull(comment.getCreatedAt());

        comment.update(new UpdateComment("  New content 2  "));

        assertEquals("New content 2", comment.getContent());
        assertEquals(user, comment.getUser());
        assertEquals(task, comment.getTask());
        assertNotNull(comment.getCreatedAt());
    }

    @Test
    void setContentTest() {
        Comment comment = new Comment(task, "Content", user);
        comment.setContent("New content");

        assertEquals("New content", comment.getContent());
        assertEquals(user, comment.getUser());
        assertEquals(task, comment.getTask());
        assertNotNull(comment.getCreatedAt());

        comment.setContent("  New content 2  ");

        assertEquals("New content 2", comment.getContent());
        assertEquals(user, comment.getUser());
        assertEquals(task, comment.getTask());
        assertNotNull(comment.getCreatedAt());
    }

    @Test
    void validationValidTest() {
        assertTrue(validator.validate(new Comment(task, "Content", user)).isEmpty());
    }

    @Test
    void validationInvalidTest() {
        assertEquals(2, validator.validate(new Comment(task, "", user)).size());
        assertEquals(2, validator.validate(new Comment(task, "  ", user)).size());
        assertEquals(2, validator.validate(new Comment(task, "   ", user)).size());
        assertEquals(1, validator.validate(new Comment(task, "a".repeat(1001), user)).size());
    }

}
