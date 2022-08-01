package com.workflow.workflow.sprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

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
public class SprintRequestTests {
    @Test
    void setNameTests() {
        SprintRequest sprintRequest = new SprintRequest();

        assertThrows(FieldErrorException.class, () -> sprintRequest.setName(null));
        assertThrows(FieldErrorException.class, () -> sprintRequest.setName("  "));
        String tooLongName = "a".repeat(51);
        assertThrows(FieldErrorException.class, () -> sprintRequest.setName(tooLongName));

        sprintRequest.setName("Test");
        assertEquals("Test", sprintRequest.getName().get());
    }

    @Test
    void setDescriptionTests() {
        SprintRequest sprintRequest = new SprintRequest();

        sprintRequest.setDescription("Test");
        assertEquals("Test", sprintRequest.getDescription().get());

        assertThrows(FieldErrorException.class, () -> sprintRequest.setDescription(null));

        sprintRequest.setDescription("  ");
        assertEquals("", sprintRequest.getDescription().get());
    }

    @Test
    void setStartDateTests() {
        SprintRequest sprintRequest = new SprintRequest();

        Date startDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
        sprintRequest.setStartDate(startDate);
        assertEquals(startDate, sprintRequest.getStartDate().get());
        assertThrows(FieldErrorException.class, () -> sprintRequest.setStartDate(null));

        SprintRequest sprintRequest2 = new SprintRequest();
        sprintRequest2.setFinishDate(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)));

        Date now = Date.from(Instant.now());
        assertThrows(FieldErrorException.class, () -> sprintRequest2.setStartDate(now));
    }

    @Test
    void setFinishDateTests() {
        SprintRequest sprintRequest = new SprintRequest();

        Date finishDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
        sprintRequest.setFinishDate(finishDate);
        assertEquals(finishDate, sprintRequest.getFinishDate().get());
        assertThrows(FieldErrorException.class, () -> sprintRequest.setFinishDate(null));

        SprintRequest sprintRequest2 = new SprintRequest();
        sprintRequest2.setStartDate(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));

        Date now = Date.from(Instant.now());
        assertThrows(FieldErrorException.class, () -> sprintRequest2.setFinishDate(now));
    }

    @Test
    void setStatusTests() {
        SprintRequest sprintRequest = new SprintRequest();

        sprintRequest.setStatus("Test");
        assertEquals("Test", sprintRequest.getStatus().get());

        assertThrows(FieldErrorException.class, () -> sprintRequest.setStatus(null));
    }
}
