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

package dev.vernite.vernite.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Where;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.vernite.vernite.cdn.File;
import dev.vernite.vernite.common.utils.counter.CounterSequence;
import dev.vernite.vernite.integration.common.Integration;
import dev.vernite.vernite.integration.git.github.entity.GitHubIntegration;
import dev.vernite.vernite.projectworkspace.ProjectWorkspace;
import dev.vernite.vernite.sprint.Sprint;
import dev.vernite.vernite.status.Status;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.utils.SoftDeleteEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entity for representing project.
 */
@Entity
@ToString
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Project extends SoftDeleteEntity implements Comparable<Project> {

    @Id
    @Setter
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PositiveOrZero(message = "project ID must be non negative number")
    private long id;

    @Getter
    @Column(nullable = false, length = 50)
    @Size(min = 1, max = 50, message = "project name must be shorter than 50 characters")
    @NotBlank(message = "project name must contain at least one non-whitespace character")
    private String name;

    @Getter
    @Column(nullable = false, length = 1000)
    @NotNull(message = "project description cannot be null")
    @Size(max = 1000, message = "project description must be shorter than 1000 characters")
    private String description;

    @Setter
    @Getter
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "project")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @NotNull(message = "project workspaces connection must be set")
    private List<ProjectWorkspace> projectWorkspaces = new ArrayList<>();

    @Setter
    @Getter
    @ManyToMany
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotNull(message = "users connection must be set")
    @JoinTable(name = "project_workspace", joinColumns = @JoinColumn(name = "project_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "workspace_user_id", referencedColumnName = "id"))
    private Set<User> users = new HashSet<>();

    @Setter
    @Getter
    @JsonIgnore
    @ToString.Exclude
    @OrderBy("ordinal")
    @EqualsAndHashCode.Exclude
    @OnDelete(action = OnDeleteAction.CASCADE)
    @NotNull(message = "project must have statuses")
    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "project")
    private List<Status> statuses = new ArrayList<>();

    @Setter
    @Getter
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotNull(message = "counter must be set")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToOne(cascade = CascadeType.PERSIST, optional = false)
    private CounterSequence taskCounter;

    @Setter
    @Getter
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotNull(message = "counter must be set")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToOne(cascade = CascadeType.PERSIST, optional = false)
    private CounterSequence sprintCounter;

    @Setter
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(mappedBy = "project")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private GitHubIntegration gitHubIntegration;

    @Setter
    @Getter
    @JsonIgnore
    @ToString.Exclude
    @OrderBy("number")
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "project")
    @Where(clause = "active is null")
    @NotNull(message = "counter must be set")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Sprint> sprints = new ArrayList<>();

    @Setter
    @Getter
    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private File logo;

    /**
     * Default constructor for project.
     * 
     * @param name        must not be {@literal null} and have size between 1 and 50
     * @param description must not be {@literal null} and must be shorter than 1000
     *                    characters.
     */
    public Project(String name, String description) {
        setName(name);
        setDescription(description);
        this.taskCounter = new CounterSequence();
        this.sprintCounter = new CounterSequence();
    }

    /**
     * Constructor for project from create request.
     * 
     * @param create must not be {@literal null} and must be valid
     */
    public Project(CreateProject create) {
        this(create.getName(), create.getDescription());
    }

    /**
     * Updates project entity with data from update.
     * 
     * @param update must not be {@literal null} and be valid
     */
    public void update(UpdateProject update) {
        if (update.getName() != null) {
            setName(update.getName());
        }
        if (update.getDescription() != null) {
            setDescription(update.getDescription());
        }
    }

    /**
     * Checks whether user is member of project.
     * 
     * @param user potential project member
     * @return {@literal true} if given user is member of project; {@literal false}
     *         otherwise
     */
    public boolean isMember(User user) {
        return getUsers().contains(user);
    }

    /**
     * Remove user from project members.
     * 
     * @param user must not be {@literal null}
     * @return removed connection; can be null if user wasn't member
     */
    public ProjectWorkspace removeMember(User user) {
        ProjectWorkspace removed = getProjectWorkspaces().stream()
                .filter(pw -> pw.getId().getWorkspaceId().getUserId() == user.getId()).findFirst().orElse(null);
        getProjectWorkspaces().remove(removed);
        return removed;
    }

    /**
     * Return all integrations for project.
     * 
     * @return list of integrations for project
     */
    public List<Integration> getIntegrations() {
        if (getGitHubIntegrationEntity() == null) {
            return List.of();
        }
        return List.of(new dev.vernite.vernite.integration.git.github.integration.GitHubProjectIntegration(
                getGitHubIntegrationEntity().getId(),
                getGitHubIntegrationEntity().getActive() == null,
                getGitHubIntegrationEntity().getRepositoryFullName()));
    }

    /**
     * Find index of user in project workspace list.
     * 
     * @param user must not be {@literal null}. Must be value returned by
     *             repository.
     * @return index in project workspaces with given user or -1 when not found.
     */
    @Deprecated
    public int member(User user) {
        ListIterator<ProjectWorkspace> iterator = projectWorkspaces.listIterator();
        while (iterator.hasNext()) {
            if (iterator.next().getId().getWorkspaceId().getUserId() == user.getId()) {
                return iterator.nextIndex() - 1;
            }
        }
        return -1;
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

    /**
     * Setter for description value. It performs {@link String#trim()} on its
     * argument.
     * 
     * @param description must not be {@literal null} and have at least one
     *                    non-whitespace character and less than 50 characters
     */
    public void setDescription(String description) {
        this.description = description.trim();
    }

    @Deprecated
    public String getGitHubIntegration() {
        return gitHubIntegration != null && gitHubIntegration.getActive() == null
                ? gitHubIntegration.getRepositoryFullName()
                : null;
    }

    @JsonIgnore
    public GitHubIntegration getGitHubIntegrationEntity() {
        return gitHubIntegration;
    }

    @Override
    @Deprecated
    public int compareTo(Project other) {
        return getName().equals(other.getName()) ? Long.compare(getId(), other.getId())
                : getName().compareTo(other.getName());
    }

    @Deprecated
    public Project(String name) {
        this(name, "");
        this.statuses.add(new Status("To Do", 0, 0, false, true, this));
        this.statuses.add(new Status("In Progress", 0, 1, false, false, this));
        this.statuses.add(new Status("Done", 0, 2, true, false, this));
    }

}
