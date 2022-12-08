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

import java.util.Collection;
import java.util.Date;

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.user.User;

/**
 * Interface for providing events. All classes implementing this interface will
 * be automatically registered as event providers.
 */
public interface EventProvider {

    /**
     * Provides events for the given user between dates.
     * 
     * @param user      the user
     * @param startDate the start date; if null, all events before the end date will
     *                  be returned
     * @param endDate   the end date; if null, all events after the start date will
     *                  be returned
     * @param filter    the filter
     * @return an collection of events
     */
    Collection<Event> provideUserEvents(User user, Date startDate, Date endDate, EventFilter filter);

    /**
     * Provides events for the given project between dates.
     * 
     * @param project   the project
     * @param startDate the start date; if null, all events before the end date will
     *                  be returned
     * @param endDate   the end date; if null, all events after the start date will
     *                  be returned
     * @param filter    the filter
     * @return an collection of events
     */
    Collection<Event> provideProjectEvents(Project project, Date startDate, Date endDate, EventFilter filter);

}
