package com.workflow.workflow.sprint;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.utils.SoftDeleteEntity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
public class Sprint extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, length = 50)
    private String name;

    private Date startDate;
    private Date finishDate;

    @Column(nullable = false)
    private String status;

    @Lob
    @Column(nullable = false)
    private String description;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "project_id", foreignKey = @ForeignKey(name = "fk_sprint_project"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Project project;

    @OneToMany(mappedBy = "sprint", fetch = FetchType.LAZY)
    @OrderBy("name ASC")
    private List<Task> tasks;

    public Sprint() {
    }

    public Sprint(String name, Date startDate, Date finishDate, String status, String description, Project project) {
        this.name = name;
        this.startDate = startDate;
        this.finishDate = finishDate;
        this.status = status;
        this.description = description;
        this.project = project;
    }

    public Sprint(SprintRequest request, Project project) {
        this(request.getName(), request.getStartDate(), request.getFinishDate(), request.getStatus(),
                request.getDescription(), project);
    }

    /**
     * Applies changes contained in request object to workspace.
     * 
     * @param request must not be {@literal null}. Can contain {@literal null} in
     *                fields. If field is {@literal null} it is assumed there is no
     *                changes for that field.
     */
    public void apply(SprintRequest request) {
        if (request.getName() != null) {
            name = request.getName();
        }
        if (request.getStartDate() != null) {
            startDate = request.getStartDate();
        }
        if (request.getFinishDate() != null) {
            finishDate = request.getFinishDate();
        }
        if (request.getStatus() != null) {
            status = request.getStatus();
        }
        if (request.getDescription() != null) {
            description = request.getDescription();
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(Date finishDate) {
        this.finishDate = finishDate;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        Sprint other = (Sprint) obj;
        if (getId() != other.getId())
            return false;
        return getName().equals(other.getName());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hash = prime + Long.hashCode(getId());
        hash = prime * hash + (getName() == null ? 0 : getName().hashCode());
        return hash;
    }
}
