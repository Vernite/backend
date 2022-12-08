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

package dev.vernite.vernite.release;

import java.util.Collection;
import java.util.Date;

import org.springframework.stereotype.Service;

import dev.vernite.vernite.event.Event;
import dev.vernite.vernite.event.EventFilter;
import dev.vernite.vernite.event.EventProvider;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.user.User;
import lombok.AllArgsConstructor;

/**
 * Event provider for releases.
 */
@Service
@AllArgsConstructor
public class ReleaseEventProvider implements EventProvider {

    private static Event convert(Release release) {
        return new Event(release.getProject().getId(), Event.Type.RELEASE, release.getId(), release.getName(),
                release.getDescription(), null, release.getDeadline(), null);
    }

    private ReleaseRepository repository;

    @Override
    public Collection<Event> provideUserEvents(User user, Date startDate, Date endDate, EventFilter filter) {
        return repository.findAllFromUserAndDate(user, startDate, endDate).stream()
                .map(ReleaseEventProvider::convert).toList();
    }

    @Override
    public Collection<Event> provideProjectEvents(Project project, Date startDate, Date endDate, EventFilter filter) {
        return repository.findAllFromProjectAndDate(project, startDate, endDate).stream()
                .map(ReleaseEventProvider::convert).toList();
    }

    @Override
    public String getType() {
        return Event.Type.RELEASE.name();
    }

}
