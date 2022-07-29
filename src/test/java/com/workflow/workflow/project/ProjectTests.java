package com.workflow.workflow.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
class ProjectTests {
    @Test
    void equalsTest() {
        Project project = new Project("name");
        Project other = new Project("other");

        assertEquals(false, project.equals(null));
        assertNotEquals(new Object(), project);
        assertNotEquals(other, project);

        other.setName("name");
        other.setId(1);

        assertNotEquals(other, project);

        other.setId(project.getId());
        assertEquals(other, project);

        project.setName(null);
        assertNotEquals(other, project);

        other.setName(null);
        assertEquals(other, project);

        project.setName("name");
        assertNotEquals(other, project);
    }

    @Test
    void hashCodeTest() {
        Project project = new Project("name");
        Project other = new Project("name");

        other.setId(1);
        assertNotEquals(other.hashCode(), project.hashCode());

        other.setId(project.getId());
        assertEquals(other.hashCode(), project.hashCode());
    }

    @Test
    void compareToTest() {
        Project project = new Project("name");
        Project other = new Project("other");

        assertEquals(true, project.compareTo(other) < 0);

        other.setName("name");
        other.setId(1);

        assertEquals(true, project.compareTo(other) < 0);

        other.setId(project.getId());

        assertEquals(0, project.compareTo(other));
    }
}
