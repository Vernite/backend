package com.workflow.workflow.project;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;

public class ProjectInvite {
    private List<String> emails;
    @Schema(accessMode = AccessMode.WRITE_ONLY)
    private List<Long> projects;
    @Schema(accessMode = AccessMode.READ_ONLY)
    private List<Project> projectList;

    public ProjectInvite() {
    }

    public ProjectInvite(List<String> emails, List<Project> projects) {
        this.emails = emails;
        this.projectList = projects;
    }

    public List<String> getEmails() {
        return emails == null ? List.of() : emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public List<Long> getProjects() {
        return projects == null ? List.of() : projects;
    }

    public void setProjects(List<Long> projects) {
        this.projects = projects;
    }

    public List<Project> getProjectList() {
        return projectList;
    }

    public void setProjectList(List<Project> projectList) {
        this.projectList = projectList;
    }
}
