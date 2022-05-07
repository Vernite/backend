package com.workflow.workflow.integration.git.github;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import com.workflow.workflow.project.Project;

@Entity
public class GitHubIntegration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    @OneToOne(optional = false)
    private Project project;
    @ManyToOne
    private GitHubInstallation installation;
    private long repositoryId;

    public GitHubIntegration() {
    }

    public GitHubIntegration(Project project, GitHubInstallation installation, long repositoryId) {
        this.project = project;
        this.installation = installation;
        this.repositoryId = repositoryId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public GitHubInstallation getInstallation() {
        return installation;
    }

    public void setInstallation(GitHubInstallation installation) {
        this.installation = installation;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(long repositoryId) {
        this.repositoryId = repositoryId;
    }
}
