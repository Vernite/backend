package com.workflow.workflow.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.workflow.workflow.integration.git.IssueAction;
import com.workflow.workflow.integration.git.PullAction;
import com.workflow.workflow.utils.FieldErrorException;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
class TaskRequestTests {
    @Test
    void setNameTests() {
        TaskRequest taskRequest = new TaskRequest();

        assertThrows(FieldErrorException.class, () -> taskRequest.setName(null));
        assertThrows(FieldErrorException.class, () -> taskRequest.setName("  "));
        String tooLongName = "a".repeat(51);
        assertThrows(FieldErrorException.class, () -> taskRequest.setName(tooLongName));

        taskRequest.setName("Test");
        assertEquals("Test", taskRequest.getName().get());
    }

    @Test
    void setDescriptionTests() {
        TaskRequest taskRequest = new TaskRequest();

        taskRequest.setDescription("Test");
        assertEquals("Test", taskRequest.getDescription().get());

        assertThrows(FieldErrorException.class, () -> taskRequest.setDescription(null));

        taskRequest.setDescription("  ");
        assertEquals("", taskRequest.getDescription().get());
    }

    @Test
    void setStatusIdTests() {
        TaskRequest taskRequest = new TaskRequest();

        taskRequest.setStatusId(1L);
        assertEquals(1, taskRequest.getStatusId().get());
        assertThrows(FieldErrorException.class, () -> taskRequest.setStatusId(null));

        taskRequest.setStatusId(0L);
        assertEquals(0, taskRequest.getStatusId().get());
    }

    @Test
    void setTypeTests() {
        TaskRequest taskRequest = new TaskRequest();

        taskRequest.setType(0);
        assertEquals(0, taskRequest.getType().get());

        assertThrows(FieldErrorException.class, () -> taskRequest.setType(null));
    }

    @Test
    void setIssueTests() {
        TaskRequest taskRequest = new TaskRequest();

        taskRequest.setIssue(IssueAction.CREATE);
        assertEquals(IssueAction.CREATE, taskRequest.getIssue().get());

        taskRequest.setIssue(IssueAction.ATTACH);
        assertEquals(IssueAction.ATTACH, taskRequest.getIssue().get());
    }

    @Test
    void setPullTests() {
        TaskRequest taskRequest = new TaskRequest();

        taskRequest.setPull(PullAction.ATTACH);
        assertEquals(PullAction.ATTACH, taskRequest.getPull().get());

        taskRequest.setPull(PullAction.DETACH);
        assertEquals(PullAction.DETACH, taskRequest.getPull().get());
    }

    @Test
    void setPriorityTests() {
        TaskRequest taskRequest = new TaskRequest();

        taskRequest.setPriority("low");
        assertEquals("low", taskRequest.getPriority().get());
        assertThrows(FieldErrorException.class, () -> taskRequest.setPriority(null));
    }

    @Test
    void createEntityTests() {
        TaskRequest taskRequest = new TaskRequest();

        assertThrows(FieldErrorException.class, () -> taskRequest.createEntity(1, null, null));

        TaskRequest taskRequest2 = new TaskRequest("name", "description", 1L, 1, "low");
        Task task = taskRequest2.createEntity(1, null, null);

        assertEquals("name", task.getName());
    }
}
