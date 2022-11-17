package dev.vernite.vernite.integration.calendar;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.user.User;

public interface CalendarIntegrationRepository extends CrudRepository<CalendarIntegration, Long> {
    Optional<CalendarIntegration> findByKey(String key);

    Optional<CalendarIntegration> findByUserAndProject(User user, Project project);

    Optional<CalendarIntegration> findByUserAndProjectNull(User user);
}
