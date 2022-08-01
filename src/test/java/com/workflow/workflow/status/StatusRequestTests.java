package com.workflow.workflow.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.utils.FieldErrorException;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
class StatusRequestTests {
    @Test
    void setNameTests() {
        StatusRequest statusRequest = new StatusRequest();

        statusRequest.setName("Test");
        assertEquals("Test", statusRequest.getName().get());

        assertThrows(FieldErrorException.class, () -> statusRequest.setName(null));
        assertThrows(FieldErrorException.class, () -> statusRequest.setName("  "));

        String tooLong = "a".repeat(51);
        assertThrows(FieldErrorException.class, () -> statusRequest.setName(tooLong));
    }

    @Test
    void setColorTests() {
        StatusRequest statusRequest = new StatusRequest();

        statusRequest.setColor(0);
        assertEquals(0, statusRequest.getColor().get());

        assertThrows(FieldErrorException.class, () -> statusRequest.setColor(null));
    }

    @Test
    void setIsFinalTests() {
        StatusRequest statusRequest = new StatusRequest();

        statusRequest.setIsFinal(true);
        assertEquals(true, statusRequest.getIsFinal().get());

        assertThrows(FieldErrorException.class, () -> statusRequest.setIsFinal(null));
    }

    @Test
    void setIsBeginTests() {
        StatusRequest statusRequest = new StatusRequest();

        statusRequest.setIsBegin(true);
        assertEquals(true, statusRequest.getIsBegin().get());

        assertThrows(FieldErrorException.class, () -> statusRequest.setIsBegin(null));
    }

    @Test
    void setOrdinalTests() {
        StatusRequest statusRequest = new StatusRequest();

        statusRequest.setOrdinal(1);
        assertEquals(1, statusRequest.getOrdinal().get());

        assertThrows(FieldErrorException.class, () -> statusRequest.setOrdinal(null));
    }

    @Test
    void createEntityTests() {
        Project project = new Project();
        StatusRequest statusRequest = new StatusRequest();

        assertThrows(FieldErrorException.class, () -> statusRequest.createEntity(1, project));

        StatusRequest statusRequest2 = new StatusRequest("Test", 0, true, true, 1);
        Status status = statusRequest2.createEntity(1, project);

        assertEquals("Test", status.getName());
    }
}
