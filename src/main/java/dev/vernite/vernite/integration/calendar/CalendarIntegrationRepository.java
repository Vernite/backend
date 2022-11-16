package dev.vernite.vernite.integration.calendar;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface CalendarIntegrationRepository extends CrudRepository<CalendarIntegration, Long> {
    Optional<CalendarIntegration> findByKey(String key);
}
