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

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import dev.vernite.vernite.task.Task;
import dev.vernite.vernite.user.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Entity for representing comment for task.
 */
@Data
@Entity
@NoArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PositiveOrZero(message = "comment ID must be non negative number")
    private long id;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @NotNull(message = "comment must be connected to task")
    private Task task;

    @Column(nullable = false, length = 1000)
    @NotNull(message = "comment content cannot be null")
    @Size(max = 1000, message = "comment must be shorter than 1000 characters")
    private String content;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @NotNull(message = "comment must have an author")
    private User user;

    @NotNull(message = "comment must have creation date")
    private Date createdAt;

    /**
     * Creates comment with given content and author. Sets creation date to current
     * time.
     * 
     * @param task    task to which comment is connected
     * @param content content of comment
     * @param user    author of comment
     */
    public Comment(Task task, String content, User user) {
        this.task = task;
        setContent(content);
        this.user = user;
        this.createdAt = new Date();
    }

    /**
     * Creates comment with given content and author. Sets creation date to current
     * time. Gets content from create request.
     * 
     * @param task   task to which comment is connected
     * @param user   author of comment
     * @param create create request
     */
    public Comment(Task task, User user, CreateComment create) {
        this(task, create.getContent(), user);
    }

    /**
     * Updates comment entity with data from update.
     * 
     * @param update must not be {@literal null} and be valid
     */
    public void update(UpdateComment update) {
        if (update.getContent() != null) {
            setContent(update.getContent());
        }
    }

    /**
     * Sets content of comment. Trims content before setting it.
     * 
     * @param content content of comment
     */
    public void setContent(String content) {
        this.content = content.trim();
    }

}
