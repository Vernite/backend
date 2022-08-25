package com.workflow.workflow.task.time;

import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.workflow.workflow.task.Task;
import com.workflow.workflow.user.User;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
class TimeTrackKeyTests {
    private static final User USER = new User("name", "surname", "username", "email", "password");
    private static final Task TASK = new Task(1, "name", "description", null, USER, 0);

    @Test
    void equalsTest() {
        Task task2 = new Task(2, "name", "description", null, USER, 0);
        task2.setId(2);
        
        TimeTrackKey timeTrackKey = new TimeTrackKey(USER, TASK);
        TimeTrackKey timeTrackKey2 = new TimeTrackKey(USER, TASK);

        assertEquals(timeTrackKey, timeTrackKey2);
        assertNotEquals(new TimeTrackKey(USER, task2), timeTrackKey);

        timeTrackKey2.setUserId(999);
        assertNotEquals(timeTrackKey, timeTrackKey2);

        assertNotEquals(null, timeTrackKey);
        assertNotEquals(new Object(), timeTrackKey);
        assertEquals(false, timeTrackKey.equals(null));

        assertEquals(timeTrackKey, timeTrackKey);
    }

    @Test
    void hashCodeTest() {
        TimeTrackKey timeTrackKey = new TimeTrackKey(USER, TASK);
        TimeTrackKey timeTrackKey2 = new TimeTrackKey(USER, TASK);

        assertEquals(timeTrackKey.hashCode(), timeTrackKey2.hashCode());

        timeTrackKey2.setUserId(999);
        assertNotEquals(timeTrackKey.hashCode(), timeTrackKey2.hashCode());

        timeTrackKey2.setUserId(USER.getId());
        timeTrackKey2.setTaskId(2);

        assertNotEquals(timeTrackKey.hashCode(), timeTrackKey2.hashCode());

        assertEquals(timeTrackKey.hashCode(), timeTrackKey.hashCode());
    }

    @Test
    void compareToTest() {
        TimeTrackKey timeTrackKey = new TimeTrackKey(USER, TASK);
        TimeTrackKey timeTrackKey2 = new TimeTrackKey(USER, TASK);

        assertEquals(0, timeTrackKey.compareTo(timeTrackKey2));

        timeTrackKey2.setUserId(999);
        assertNotEquals(0, timeTrackKey.compareTo(timeTrackKey2));
        assertEquals(true, timeTrackKey.compareTo(timeTrackKey2) < 0);

        timeTrackKey2.setUserId(USER.getId());
        timeTrackKey2.setTaskId(2);
        assertNotEquals(0, timeTrackKey.compareTo(timeTrackKey2));
        assertEquals(true, timeTrackKey.compareTo(timeTrackKey2) < 0);

    }
}
