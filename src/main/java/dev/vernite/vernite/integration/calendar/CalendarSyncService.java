package dev.vernite.vernite.integration.calendar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import dev.vernite.vernite.event.Event;
import dev.vernite.vernite.event.EventFilter;
import dev.vernite.vernite.event.EventService;
import dev.vernite.vernite.utils.ObjectNotFoundException;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Name;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

@Service
@Component
public class CalendarSyncService {
    private static final java.util.Date FROM = new java.util.Date(0);

    @Autowired
    private EventService eventService;
    @Autowired
    private CalendarIntegrationRepository repository;

    public byte[] handleCalendar(String key) {
        java.util.Date to = java.util.Date.from(Instant.now().plus(1000, ChronoUnit.DAYS));
        CalendarIntegration integration = repository.findByKey(key).orElseThrow(ObjectNotFoundException::new);
        List<Event> events;
        Calendar calendar = new Calendar();
        if (integration.getProject() == null) {
            events = eventService.getUserEvents(integration.getUser(), FROM, to, new EventFilter());
            calendar.getProperties()
                    .add(new Name("Vernite - " + integration.getUser().getUsername() + " user calendar"));
        } else {
            events = eventService.getProjectEvents(integration.getProject(), FROM, to, new EventFilter());
            calendar.getProperties()
                    .add(new Name("Vernite - " + integration.getProject().getName() + " project calendar"));
        }
        calendar.getProperties().add(new ProdId("-//Vernite//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);
        for (Event event : events) {
            VEvent calEvent;
            if (event.getStartDate() == null) {
                calEvent = new VEvent(new Date(event.getEndDate()), event.getName());
            } else {
                calEvent = new VEvent(new Date(event.getStartDate()), new Date(event.getEndDate()), event.getName());
            }
            calEvent.getProperties().add(new Uid(String.format("project_%d_event_%d_%d", event.getProjectId(),
                    event.getEventType().ordinal(), event.getRelatedId())));
            if (event.getDescription() != null) {
                calEvent.getProperties().add(new Description(event.getDescription()));
            }
            if (event.getLocation() != null) {
                calEvent.getProperties().add(new Location(event.getLocation()));
            }
            calendar.getComponents().add(calEvent);
        }
        CalendarOutputter calendarOutputter = new CalendarOutputter();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            calendarOutputter.output(calendar, stream);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "IO error");
        }
        return stream.toByteArray();
    }
}
