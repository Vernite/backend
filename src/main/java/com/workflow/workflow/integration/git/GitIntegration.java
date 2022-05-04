package com.workflow.workflow.integration.git;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import com.workflow.workflow.integration.ApiToken;
import com.workflow.workflow.project.Project;

@Entity
public class GitIntegration {
    private @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @OneToOne
    private Project project;
    @ManyToOne
    private ApiToken apiToken;
    private String apiLink;
    private long repositoryId;

    public GitIntegration() {}

    public GitIntegration(Project project, ApiToken apiToken, String apiLink, long repositoryId) {
        this.project = project;
        this.apiToken = apiToken;
        this.apiLink = apiLink;
        this.repositoryId = repositoryId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public ApiToken getApiToken() {
        return apiToken;
    }

    public void setApiToken(ApiToken apiToken) {
        this.apiToken = apiToken;
    }

    public String getApiLink() {
        return apiLink;
    }

    public void setApiLink(String apiLink) {
        this.apiLink = apiLink;
    }

    public long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(long repositoryId) {
        this.repositoryId = repositoryId;
    }
}
