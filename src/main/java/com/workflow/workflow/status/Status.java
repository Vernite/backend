package com.workflow.workflow.status;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.utils.SoftDeleteEntity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
public class Status extends SoftDeleteEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private long id;
    
    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer color;

    @Column(nullable = false)
    private Boolean isFinal;

    @Column(nullable = false)
    private boolean isBegin;
    
    @Column(nullable = false)
    private Integer ordinal;

    @JsonIgnore
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_column_project"))
    private Project project;

    public Status() {
    }

    public Status(String name, Integer color, Boolean isFinal, boolean isBegin, Integer ordinal, Project project) {
        this.name = name;
        this.color = color;
        this.isFinal = isFinal;
        this.isBegin = isBegin;
        this.ordinal = ordinal;
        this.project = project;
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

    public Integer getColor() {
        return color;
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    public Boolean isFinal() {
        return isFinal;
    }

    public void setFinal(Boolean isFinal) {
        this.isFinal = isFinal;
    }

    public boolean isBegin() {
        return isBegin;
    }

    public void setBegin(boolean isBegin) {
        this.isBegin = isBegin;
    }

    public Integer getOrdinal() {
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

    public Status apply(Status o) {
        if (o.getColor() != null) {
            this.setColor(o.getColor());
        }
        if (o.getName() != null) {
            this.setName(o.getName());
        }
        if (o.getOrdinal() != null) {
            this.setOrdinal(o.getOrdinal());
        }
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getColor(), getId(), isBegin(), isFinal(), getName(), getOrdinal(), getProject());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Status))
            return false;
        Status other = (Status) obj;
        return Objects.equals(getColor(), other.getColor()) && getId() == other.getId() && isBegin() == other.isBegin()
                && Objects.equals(isFinal(), other.isFinal()) && Objects.equals(getName(), other.getName())
                && Objects.equals(getOrdinal(), other.getOrdinal()) && Objects.equals(getProject(), other.getProject());
    }
}
