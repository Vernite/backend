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

package dev.vernite.vernite.workspace;

import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.projectworkspace.ProjectWorkspace;
import dev.vernite.vernite.user.User;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Where;

/**
 * Entity for representing collection of projects.
 * Its connected to user and has unique id for that user.
 */
@Entity
@ToString
@NoArgsConstructor
@EqualsAndHashCode
public class Workspace {

    @Valid
    @Setter
    @Getter
    @EmbeddedId
    @JsonUnwrapped
    @NotNull(message = "workspace id cannot be null")
    private WorkspaceId id;

    @Getter
    @Column(nullable = false, length = 50)
    @Size(min = 1, max = 50, message = "workspace name must be shorter than 50 characters")
    @NotBlank(message = "workspace name must contain at least one non-whitespace character")
    private String name;

    @Setter
    @Getter
    @JsonIgnore
    @MapsId("userId")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false)
    @NotNull(message = "workspace must have user set")
    private User user;

    @Setter
    @Getter
    @Deprecated
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "workspace")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @NotNull(message = "project workspaces connection must be set")
    private List<ProjectWorkspace> projectWorkspaces = List.of();

    @Setter
    @Getter
    @ManyToMany
    @ToString.Exclude
    @OrderBy("name, id")
    @EqualsAndHashCode.Exclude
    @Where(clause = "active is null")
    @NotNull(message = "projects connection must be set")
    @JoinTable(name = "project_workspace", joinColumns = {
            @JoinColumn(name = "workspace_user_id", referencedColumnName = "user_id"),
            @JoinColumn(name = "workspace_id", referencedColumnName = "id"),
    }, inverseJoinColumns = @JoinColumn(name = "project_id", referencedColumnName = "id"))
    private Set<Project> projects = Set.of();

    /**
     * Base constructor for workspace.
     * 
     * @param id   unique to user positive number for new workspace
     * @param name must not be {@literal null} and have size between 1 and 50
     * @param user must not be {@literal null} and must be entity from database
     */
    public Workspace(long id, String name, User user) {
        this.setId(new WorkspaceId(id, user.getId()));
        this.setName(name);
        this.setUser(user);
    }

    /**
     * Constructor for workspace from create request.
     * 
     * @param id     unique to user positive number for new workspace
     * @param user   must not be {@literal null} and must be entity from database
     * @param create must not be {@literal null} and must be valid
     */
    public Workspace(long id, User user, CreateWorkspace create) {
        this(id, create.getName(), user);
    }

    /**
     * Updates workspace entity with data from update.
     * 
     * @param update must not be {@literal null} and be valid
     */
    public void update(UpdateWorkspace update) {
        if (update.getName() != null) {
            setName(update.getName());
        }
    }

    /**
     * Setter for name value. It performs {@link String#trim()} on its argument.
     * 
     * @param name must not be {@literal null} and have at least one non-whitespace
     *             character and less than 50 characters
     */
    public void setName(String name) {
        this.name = name.trim();
    }

}
