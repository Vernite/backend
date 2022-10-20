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

package com.workflow.workflow.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.workflow.workflow.task.Task.TaskType;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
class TaskTypeTests {
    @Test
    void isValidParentEpicTests() {
        assertFalse(TaskType.EPIC.isValidParent(TaskType.EPIC));
        assertFalse(TaskType.EPIC.isValidParent(TaskType.ISSUE));
        assertFalse(TaskType.EPIC.isValidParent(TaskType.SUBTASK));
        assertFalse(TaskType.EPIC.isValidParent(TaskType.TASK));
        assertFalse(TaskType.EPIC.isValidParent(TaskType.USER_STORY));
    }

    @Test
    void isValidParentIssueTests() {
        assertTrue(TaskType.ISSUE.isValidParent(TaskType.EPIC));
        assertFalse(TaskType.ISSUE.isValidParent(TaskType.ISSUE));
        assertFalse(TaskType.ISSUE.isValidParent(TaskType.SUBTASK));
        assertTrue(TaskType.ISSUE.isValidParent(TaskType.TASK));
        assertFalse(TaskType.ISSUE.isValidParent(TaskType.USER_STORY));
    }

    @Test
    void isValidParentSubtaskTests() {
        assertFalse(TaskType.SUBTASK.isValidParent(TaskType.EPIC));
        assertTrue(TaskType.SUBTASK.isValidParent(TaskType.ISSUE));
        assertFalse(TaskType.SUBTASK.isValidParent(TaskType.SUBTASK));
        assertTrue(TaskType.SUBTASK.isValidParent(TaskType.TASK));
        assertTrue(TaskType.SUBTASK.isValidParent(TaskType.USER_STORY));
    }

    @Test
    void isValidParentTaskTests() {
        assertTrue(TaskType.TASK.isValidParent(TaskType.EPIC));
        assertFalse(TaskType.TASK.isValidParent(TaskType.ISSUE));
        assertFalse(TaskType.TASK.isValidParent(TaskType.SUBTASK));
        assertFalse(TaskType.TASK.isValidParent(TaskType.TASK));
        assertFalse(TaskType.TASK.isValidParent(TaskType.USER_STORY));
    }

    @Test
    void isValidParentUserStoryTests() {
        assertTrue(TaskType.USER_STORY.isValidParent(TaskType.EPIC));
        assertFalse(TaskType.USER_STORY.isValidParent(TaskType.ISSUE));
        assertFalse(TaskType.USER_STORY.isValidParent(TaskType.SUBTASK));
        assertFalse(TaskType.USER_STORY.isValidParent(TaskType.TASK));
        assertFalse(TaskType.USER_STORY.isValidParent(TaskType.USER_STORY));
    }
}
