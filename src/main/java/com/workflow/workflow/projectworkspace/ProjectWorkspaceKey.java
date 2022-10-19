/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2022, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.workflow.workflow.projectworkspace;

import java.io.Serializable;

import javax.persistence.Embeddable;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.workspace.Workspace;
import com.workflow.workflow.workspace.WorkspaceKey;

/**
 * Composite key for pivot table. Composed of workspace id and project id.
 */
@Embeddable
public class ProjectWorkspaceKey implements Serializable, Comparable<ProjectWorkspaceKey> {
    WorkspaceKey workspaceId;
    long projectId;

    public ProjectWorkspaceKey() {
    }

    public ProjectWorkspaceKey(Workspace workspace, Project project) {
        this.workspaceId = workspace.getId();
        this.projectId = project.getId();
    }

    public WorkspaceKey getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(WorkspaceKey workspaceId) {
        this.workspaceId = workspaceId;
    }

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hash = prime + Long.hashCode(projectId);
        hash = prime * hash + (workspaceId == null ? 0 : workspaceId.hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        ProjectWorkspaceKey other = (ProjectWorkspaceKey) obj;
        if (projectId != other.projectId)
            return false;
        if (workspaceId == null)
            return other.workspaceId == null;
        return workspaceId.equals(other.workspaceId);
    }

    @Override
    public int compareTo(ProjectWorkspaceKey o) {
        return projectId == o.projectId ? workspaceId.compareTo(o.workspaceId) : Long.compare(projectId, o.projectId);
    }
}
