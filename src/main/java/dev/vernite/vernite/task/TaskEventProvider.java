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

package dev.vernite.vernite.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import dev.vernite.vernite.event.Event;
import dev.vernite.vernite.event.EventFilter;
import dev.vernite.vernite.event.EventProvider;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.user.User;
import lombok.AllArgsConstructor;

/**
 * Event provider for tasks.
 */
@Service
@AllArgsConstructor
public class TaskEventProvider implements EventProvider {

    private static List<Event> convert(Task task) {
        List<Event> result = new ArrayList<>();
        if (task.getEstimatedDate() != null) {
            result.add(new Event(task.getStatus().getProject().getId(), Event.Type.TASK_ESTIMATE, task.getNumber(),
                    task.getName(), task.getDescription(), null, task.getEstimatedDate(), null));
        }
        if (task.getDeadline() != null) {
            result.add(new Event(task.getStatus().getProject().getId(), Event.Type.TASK_DEADLINE, task.getNumber(),
                    task.getName(), task.getDescription(), null, task.getDeadline(), null));
        }
        return result;
    }

    private TaskRepository repository;

    @Override
    public Collection<Event> provideUserEvents(User user, Date startDate, Date endDate, EventFilter filter) {
        if (filter.getType().isEmpty() || filter.getType().contains(Event.Type.TASK_DEADLINE.ordinal())
                || filter.getType().contains(Event.Type.TASK_ESTIMATE.ordinal())) {
            return repository.findAllFromUserAndDate(user, startDate, endDate, filter).stream()
                    .map(TaskEventProvider::convert).flatMap(List::stream).toList();
        }
        return Collections.emptyList();
    }

    @Override
    public Collection<Event> provideProjectEvents(Project project, Date startDate, Date endDate, EventFilter filter) {
        if (filter.getType().isEmpty() || filter.getType().contains(Event.Type.TASK_DEADLINE.ordinal())
                || filter.getType().contains(Event.Type.TASK_ESTIMATE.ordinal())) {
            return repository.findAllFromProjectAndDate(project, startDate, endDate, filter).stream()
                    .map(TaskEventProvider::convert).flatMap(List::stream).toList();
        }
        return Collections.emptyList();
    }

}
