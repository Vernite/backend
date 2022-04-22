package com.workflow.workflow.projectworkspace;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public record ProjectWorkspaceKey(@Column(name = "workspace_id") Long workspaceId, @Column(name = "project_id") Long projectId) implements Serializable {}
