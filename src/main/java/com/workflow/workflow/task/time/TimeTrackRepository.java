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

package com.workflow.workflow.task.time;

import java.util.List;
import java.util.Optional;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.user.User;
import com.workflow.workflow.utils.NotFoundRepository;

public interface TimeTrackRepository extends NotFoundRepository<TimeTrack, Long> {
    /**
     * Finds time tracks for a project.
     * 
     * @param project the project.
     * @return list with time tracks.
     */
    List<TimeTrack> findByTaskStatusProject(Project project);

    /**
     * Finds time tracks for a user.
     * 
     * @param user the user.
     * @return list with time tracks.
     */
    List<TimeTrack> findByUser(User user);

    /**
     * Finds time track for a task and user that is currently tracking.
     * 
     * @param user the user.
     * @param task the task.
     * @return the time track. Might be empty.
     */
    Optional<TimeTrack> findByUserAndTaskAndEndDateNull(User user, Task task);
}
