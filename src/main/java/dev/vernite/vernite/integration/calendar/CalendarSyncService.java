/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2022, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
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
                    .add(new CalendarName("Vernite - " + integration.getUser().getUsername() + " user calendar"));
        } else {
            events = eventService.getProjectEvents(integration.getProject(), FROM, to, new EventFilter());
            calendar.getProperties()
                    .add(new CalendarName("Vernite - " + integration.getProject().getName() + " project calendar"));
        }
        calendar.getProperties().add(new ProdId("-//Vernite//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);
        for (Event event : events) {
            VEvent calEvent;
            if (event.getStartDate() == null) {
                calEvent = new VEvent(new Date(event.getEndDate()), event.getName());
            } else {
                calEvent = new VEvent(new DateTime(event.getStartDate()), new DateTime(event.getEndDate()), event.getName());
            }
            calEvent.getProperties().add(new Uid(String.format("project_%d_event_%d_%d", event.getProjectId(),
                    event.getType().ordinal(), event.getRelatedId())));
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
