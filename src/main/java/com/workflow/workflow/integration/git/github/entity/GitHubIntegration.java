package com.workflow.workflow.integration.git.github.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.utils.SoftDeleteEntity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
public class GitHubIntegration extends SoftDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private GitHubInstallation installation;

    @Column(unique = true, nullable = false, length = 150)
    private String repositoryFullName;

    public GitHubIntegration() {
    }

    public GitHubIntegration(Project project, GitHubInstallation installation, String repositoryFullName) {
        this.project = project;
        this.installation = installation;
        this.repositoryFullName = repositoryFullName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public GitHubInstallation getInstallation() {
        return installation;
    }

    public void setInstallation(GitHubInstallation installation) {
        this.installation = installation;
    }

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public void setRepositoryFullName(String repositoryFullName) {
        this.repositoryFullName = repositoryFullName;
    }

    public String getRepositoryName() {
        return repositoryFullName.split("/")[1];
    }

    public String getRepositoryOwner() {
        return repositoryFullName.split("/")[0];
    }
}
