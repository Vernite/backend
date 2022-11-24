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
import java.util.function.Predicate;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.projectworkspace.ProjectWithPrivileges;
import dev.vernite.vernite.projectworkspace.ProjectWorkspace;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.utils.SoftDeleteEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Entity for representing collection of projects.
 * Its connected to user and has unique id for that user.
 */
@Entity
@ToString
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Workspace extends SoftDeleteEntity implements Comparable<Workspace> {

    private static final Predicate<ProjectWorkspace> activeProject = p -> p.getProject().getActive() == null;

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
    @JsonIgnore
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
    @EqualsAndHashCode.Exclude
    @NotNull(message = "projects connection must be set")
    @JoinTable(
        name = "project_workspace",
        joinColumns = {
            @JoinColumn(name = "workspace_user_id", referencedColumnName = "user_id"),
            @JoinColumn(name = "workspace_id", referencedColumnName = "id"),
        },
        inverseJoinColumns = @JoinColumn(name = "project_id", referencedColumnName = "id")
    )
    private List<Project> projects = List.of();

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
        this.setProjectWorkspaces(List.of());
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
            setName(name);
        }
    }

    /**
     * Checks if workspace contains any active project.
     * 
     * @return {@literal true} if there is at least one active project in workspace,
     *         {@literal false} otherwise
     */
    public boolean hasActiveProject() {
        return getProjectWorkspaces().stream().anyMatch(activeProject);
    }

    /**
     * Computes projects with privileges from project workspaces.
     * 
     * @return all projects connected to workspace with user privileges in
     *         respective project
     */
    @Deprecated
    public List<ProjectWithPrivileges> getProjectsWithPrivileges() {
        return getProjectWorkspaces().stream().filter(activeProject).map(ProjectWorkspace::getProjectWithPrivileges)
                .sorted().toList();
    }

    /**
     * Setter for name value. It performs {@link String#trim()} on its argument.
     * 
     * @param name must not be {@literal null} and have at least one non-whitespace
     *             character and less than 50 characters
     */
    public void setName(@NotNull(message = "name cannot be null") String name) {
        this.name = name.trim();
    }

    @Override
    public int compareTo(Workspace other) {
        int result = getName().compareTo(other.getName());
        return result == 0 ? getId().compareTo(other.getId()) : result;
    }

    /**
     * Updates workspace with non-empty request fields.
     * 
     * @param request must not be {@literal null}. When fields are not present in
     *                request, they are not updated.
     */
    @Deprecated
    public void update(@NotNull WorkspaceRequest request) {
        request.getName().ifPresent(this::setName);
    }

    @Deprecated
    public Workspace(long id, User user, String name) {
        this.id = new WorkspaceId(id, user);
        this.user = user;
        this.name = name;
    }

}
