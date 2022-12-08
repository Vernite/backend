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

package dev.vernite.vernite.event;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Service;

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.user.User;

/**
 * Service providing events.
 */
@Service
public class EventService implements ApplicationContextAware {

    private static Class<?> getProviderClass(String name) {
        try {
            return EventService.class.getClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            // TODO Should not happen
            return null;
        }
    }

    private Iterable<EventProvider> providers;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(EventProvider.class));

        providers = scanner.findCandidateComponents("dev.vernite.vernite").stream()
                .map(BeanDefinition::getBeanClassName).map(EventService::getProviderClass).map(context::getBean)
                .map(EventProvider.class::cast).toList();
    }

    /**
     * Returns all events for the given user between dates.
     * 
     * @param user   the user
     * @param start  the start date; if null, all events before the end date will
     *               be returned
     * @param end    the end date; if null, all events after the start date will
     *               be returned
     * @param filter the filter
     * @return an sorted set of events
     */
    public Set<Event> getUserEvents(User user, Date start, Date end, EventFilter filter) {
        var result = new TreeSet<Event>();
        providers.forEach(provider -> {
            if (filter.getType().isEmpty()
                    || filter.getType().contains(Event.Type.valueOf(provider.getType()).ordinal())) {
                result.addAll(provider.provideUserEvents(user, start, end, filter));
            }
        });
        return result;
    }

    /**
     * Returns all events for the given project between dates.
     * 
     * @param project the project
     * @param start   the start date; if null, all events before the end date will
     *                be returned
     * @param end     the end date; if null, all events after the start date will be
     *                returned
     * @param filter  the filter
     * @return an sorted set of events
     */
    public Set<Event> getProjectEvents(Project project, Date start, Date end, EventFilter filter) {
        var result = new TreeSet<Event>();
        providers.forEach(provider -> {
            if (filter.getType().isEmpty()
                    || filter.getType().contains(Event.Type.valueOf(provider.getType()).ordinal())) {
                result.addAll(provider.provideProjectEvents(project, start, end, filter));
            }
        });
        return result;
    }

}
