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

package dev.vernite.vernite.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.vernite.vernite.task.Task.Type;

class TaskTypeTests {

    @Test
    void isValidParentEpicTests() {
        assertFalse(Type.EPIC.isValidParent(Type.EPIC));
        assertFalse(Type.EPIC.isValidParent(Type.ISSUE));
        assertFalse(Type.EPIC.isValidParent(Type.SUBTASK));
        assertFalse(Type.EPIC.isValidParent(Type.TASK));
        assertFalse(Type.EPIC.isValidParent(Type.USER_STORY));
    }

    @Test
    void isValidParentIssueTests() {
        assertTrue(Type.ISSUE.isValidParent(Type.EPIC));
        assertFalse(Type.ISSUE.isValidParent(Type.ISSUE));
        assertFalse(Type.ISSUE.isValidParent(Type.SUBTASK));
        assertTrue(Type.ISSUE.isValidParent(Type.TASK));
        assertFalse(Type.ISSUE.isValidParent(Type.USER_STORY));
    }

    @Test
    void isValidParentSubtaskTests() {
        assertFalse(Type.SUBTASK.isValidParent(Type.EPIC));
        assertTrue(Type.SUBTASK.isValidParent(Type.ISSUE));
        assertFalse(Type.SUBTASK.isValidParent(Type.SUBTASK));
        assertTrue(Type.SUBTASK.isValidParent(Type.TASK));
        assertTrue(Type.SUBTASK.isValidParent(Type.USER_STORY));
    }

    @Test
    void isValidParentTaskTests() {
        assertTrue(Type.TASK.isValidParent(Type.EPIC));
        assertFalse(Type.TASK.isValidParent(Type.ISSUE));
        assertFalse(Type.TASK.isValidParent(Type.SUBTASK));
        assertFalse(Type.TASK.isValidParent(Type.TASK));
        assertFalse(Type.TASK.isValidParent(Type.USER_STORY));
    }

    @Test
    void isValidParentUserStoryTests() {
        assertTrue(Type.USER_STORY.isValidParent(Type.EPIC));
        assertFalse(Type.USER_STORY.isValidParent(Type.ISSUE));
        assertFalse(Type.USER_STORY.isValidParent(Type.SUBTASK));
        assertFalse(Type.USER_STORY.isValidParent(Type.TASK));
        assertFalse(Type.USER_STORY.isValidParent(Type.USER_STORY));
    }

}
