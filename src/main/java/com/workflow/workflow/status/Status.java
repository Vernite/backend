package com.workflow.workflow.status;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.utils.SoftDeleteEntity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
public class Status extends SoftDeleteEntity {

    @Id
    @JsonIgnore
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @JsonProperty("id")
    @Column(nullable = false)
    private long number;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int color;

    @Column(nullable = false)
    private boolean isFinal;

    @Column(nullable = false)
    private boolean isBegin;

    @Column(nullable = false)
    private int ordinal;

    @JsonIgnore
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_column_project"))
    private Project project;

    public Status() {
    }

    public Status(long id, String name, int color, boolean isFinal, boolean isBegin, int ordinal, Project project) {
        this.number = id;
        this.name = name;
        this.color = color;
        this.isFinal = isFinal;
        this.isBegin = isBegin;
        this.ordinal = ordinal;
        this.project = project;
    }

    /**
     * Updates status with non-empty request fields.
     * 
     * @param request must not be {@literal null}. When fields are not present in
     *                request, they are not updated.
     */
    public void update(@NotNull StatusRequest request) {
        request.getName().ifPresent(this::setName);
        request.getColor().ifPresent(this::setColor);
        request.getFinal().ifPresent(this::setFinal);
        request.getBegin().ifPresent(this::setBegin);
        request.getOrdinal().ifPresent(this::setOrdinal);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(Boolean isFinal) {
        this.isFinal = isFinal;
    }

    public boolean isBegin() {
        return isBegin;
    }

    public void setBegin(Boolean isBegin) {
        this.isBegin = isBegin;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(Integer ordinal) {
        this.ordinal = ordinal;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
