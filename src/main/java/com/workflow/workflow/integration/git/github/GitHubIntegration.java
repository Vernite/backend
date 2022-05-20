package com.workflow.workflow.integration.git.github;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.utils.SoftDeleteEntity;

@Entity
public class GitHubIntegration extends SoftDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    @OneToOne(optional = false)
    private Project project;
    @ManyToOne
    private GitHubInstallation installation;
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

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public void setRepositoryFullName(String repositoryFullName) {
        this.repositoryFullName = repositoryFullName;
    }
}
