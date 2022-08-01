package com.workflow.workflow.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.workflow.workflow.utils.FieldErrorException;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
class ProjectRequestTests {
    @Test
    void setNameTests() {
        ProjectRequest projectRequest = new ProjectRequest();

        projectRequest.setName("Test");
        assertEquals("Test", projectRequest.getName().get());

        assertThrows(FieldErrorException.class, () -> projectRequest.setName(null));
        assertThrows(FieldErrorException.class, () -> projectRequest.setName("  "));

        String tooLong = "a".repeat(51);
        assertThrows(FieldErrorException.class, () -> projectRequest.setName(tooLong));
    }

    @Test
    void setWorkspaceIdTests() {
        ProjectRequest projectRequest = new ProjectRequest();

        projectRequest.setWorkspaceId(1L);
        assertEquals(1, projectRequest.getWorkspaceId().get());

        assertThrows(FieldErrorException.class, () -> projectRequest.setWorkspaceId(null));
    }

    @Test
    void createEntityTests() {
        ProjectRequest projectRequest = new ProjectRequest();

        assertThrows(FieldErrorException.class, () -> projectRequest.createEntity());

        ProjectRequest projectRequest2 = new ProjectRequest("Test", 1L);
        Project project = projectRequest2.createEntity();

        assertEquals("Test", project.getName());
    }
}
