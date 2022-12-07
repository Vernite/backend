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

package dev.vernite.vernite.task.comment;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.task.TaskRepository;
import dev.vernite.vernite.user.User;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;

/**
 * Rest controller for performing CRUD operations on Projects entities.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/project/{projectId}/task/{taskId}/comment")
public class CommentController {

    private ProjectRepository projectRepository;

    private TaskRepository taskRepository;

    private CommentRepository commentRepository;

    /**
     * Creates a new comment for a task.
     * 
     * @param user      the user who is creating the comment
     * @param projectId the id of the project the task belongs to
     * @param taskId    the number of the task
     * @param create    data for new comment
     * @return newly created comment
     */
    @PostMapping
    public Comment create(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long taskId, @RequestBody CreateComment create) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        var task = taskRepository.findByProjectAndNumberOrThrow(project, taskId);
        var comment = new Comment(task, user, create);
        return commentRepository.save(comment);
    }

    /**
     * Retrieves all comments for a task.
     * 
     * @param user      logged in user
     * @param projectId id of the project the task belongs to
     * @param taskId    number of the task
     * @return list of comments
     */
    @GetMapping
    public List<Comment> getAll(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long taskId) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        var task = taskRepository.findByProjectAndNumberOrThrow(project, taskId);
        return task.getComments();
    }

    /**
     * Retrieves a single comment.
     * 
     * @param user      logged in user
     * @param projectId id of the project the task belongs to
     * @param taskId    number of the task
     * @param id        id of the comment
     * @return comment
     */
    @GetMapping("/{id}")
    public Comment get(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long taskId, @PathVariable long id) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        var task = taskRepository.findByProjectAndNumberOrThrow(project, taskId);
        return commentRepository.findByIdAndTaskOrThrow(id, task);
    }

    /**
     * Updates a comment. Performs partial update using only supplied
     * fields from request body.
     * 
     * @param user      logged in user
     * @param projectId id of the project the task belongs to
     * @param taskId    number of the task
     * @param id        id of the comment
     * @param update    data to update
     * @return updated comment
     */
    @PutMapping("/{id}")
    public Comment update(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long taskId, @PathVariable long id, @RequestBody UpdateComment update) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        var task = taskRepository.findByProjectAndNumberOrThrow(project, taskId);
        var comment = commentRepository.findByIdAndTaskOrThrow(id, task);
        comment.update(update);
        return commentRepository.save(comment);
    }

    /**
     * Deletes a comment.
     * 
     * @param user      logged in user
     * @param projectId id of the project the task belongs to
     * @param taskId    number of the task
     * @param id        id of the comment
     */
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long taskId, @PathVariable long id) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        var task = taskRepository.findByProjectAndNumberOrThrow(project, taskId);
        var comment = commentRepository.findByIdAndTaskOrThrow(id, task);
        commentRepository.delete(comment);
    }

}
