package com.workflow.workflow.workspace;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.workflow.workflow.user.User;
import com.workflow.workflow.utils.FieldErrorException;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
public class WorkspaceRequestTests {
    private static final User user = new User("name", "surname", "username", "email", "password");

    @Test
    void setNameTest() {
        WorkspaceRequest workspaceRequest = new WorkspaceRequest();
        workspaceRequest.setName("Name");
        assertEquals("Name", workspaceRequest.getName().orElseThrow());

        assertThrows(FieldErrorException.class, () -> workspaceRequest.setName(null));
        assertThrows(FieldErrorException.class, () -> workspaceRequest.setName(""));
        String tooLongName = "a".repeat(51);
        assertThrows(FieldErrorException.class, () -> workspaceRequest.setName(tooLongName));
    }

    @Test
    void createEntityTests() {
        WorkspaceRequest workspaceRequest = new WorkspaceRequest("Name");
        Workspace workspace = workspaceRequest.createEntity(1, user);

        assertEquals("Name", workspace.getName());
        assertEquals(user, workspace.getUser());
        assertEquals(1, workspace.getId().getId());

        WorkspaceRequest workspaceRequest2 = new WorkspaceRequest();
        assertThrows(FieldErrorException.class, () -> workspaceRequest2.createEntity(1, user));
    }
}
