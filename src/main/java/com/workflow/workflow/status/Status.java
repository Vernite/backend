package com.workflow.workflow.status;

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

@Entity
public class Status {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer color;

    @Column(nullable = false)
    private Boolean isFinal;
    
    @Column(nullable = false)
    private Integer ordinal;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_column_project"))
    private Project project;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
}
