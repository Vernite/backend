package com.workflow.workflow.workspace;

import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.workflow.workflow.user.User;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
public class WorkspaceKeyTests {
    private static final User user = new User("name", "surname", "username", "email", "password");

    @Test
    void equalsTest() {
        WorkspaceKey workspaceKey = new WorkspaceKey(1, user);
        WorkspaceKey workspaceKey2 = new WorkspaceKey(1, user);

        assertEquals(workspaceKey, workspaceKey2);
        assertNotEquals(new WorkspaceKey(2, user), workspaceKey);

        workspaceKey2.setUserId(999);
        assertNotEquals(workspaceKey, workspaceKey2);

        assertNotEquals(null, workspaceKey);
        assertNotEquals(new Object(), workspaceKey);
        assertEquals(false, workspaceKey.equals(null));
    }

    @Test
    void hashCodeTest() {
        WorkspaceKey workspaceKey = new WorkspaceKey(1, user);
        WorkspaceKey workspaceKey2 = new WorkspaceKey(1, user);

        assertEquals(workspaceKey.hashCode(), workspaceKey2.hashCode());

        workspaceKey2.setUserId(999);
        assertNotEquals(workspaceKey.hashCode(), workspaceKey2.hashCode());

        workspaceKey2.setUserId(user.getId());
        workspaceKey2.setId(2);

        assertNotEquals(workspaceKey.hashCode(), workspaceKey2.hashCode());

        assertEquals(workspaceKey.hashCode(), workspaceKey.hashCode());
    }

    @Test
    void compareToTest() {
        WorkspaceKey workspaceKey = new WorkspaceKey(1, user);
        WorkspaceKey workspaceKey2 = new WorkspaceKey(1, user);

        assertEquals(0, workspaceKey.compareTo(workspaceKey2));

        workspaceKey2.setUserId(999);
        assertNotEquals(0, workspaceKey.compareTo(workspaceKey2));
        assertEquals(true, workspaceKey.compareTo(workspaceKey2) < 0);

        workspaceKey2.setUserId(user.getId());
        workspaceKey2.setId(2);
        assertNotEquals(0, workspaceKey.compareTo(workspaceKey2));
        assertEquals(true, workspaceKey.compareTo(workspaceKey2) < 0);

    }
}
