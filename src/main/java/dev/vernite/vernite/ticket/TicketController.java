/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2023, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
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

package dev.vernite.vernite.ticket;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.vernite.vernite.common.utils.counter.CounterSequenceRepository;
import dev.vernite.vernite.integration.git.GitTaskService;
import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.task.Task;
import dev.vernite.vernite.task.TaskRepository;
import dev.vernite.vernite.user.User;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * The controller that handles ticket-related requests.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ticket")
public class TicketController {

    private TaskRepository taskRepository;

    private CounterSequenceRepository counterSequenceRepository;

    private ProjectRepository projectRepository;

    private GitTaskService gitTaskService;

    /**
     * Creates a new ticket.
     * 
     * @param user         the user that creates the ticket
     * @param createTicket the ticket to create
     * @return the created ticket
     */
    @PostMapping
    public Mono<CreateTicket> createTicket(@NotNull @Parameter(hidden = true) User user,
            @RequestBody @Valid CreateTicket createTicket) {
        var project = projectRepository.findById(1L).orElseThrow();
        var id = counterSequenceRepository.getIncrementCounter(project.getTaskCounter().getId());
        var status = project.getStatuses().stream().filter(x -> x.isBegin()).findFirst().get();
        var title = createTicket.getTitle();
        var description = createTicket.getDescription();
        var task = new Task(id, title, description, status, user, Task.Type.ISSUE.ordinal());
        taskRepository.save(task);
        gitTaskService.createIssue(task);
        return gitTaskService.createIssue(task).then().thenReturn(createTicket);
    }

}
