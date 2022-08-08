package com.workflow.workflow.integration.git.github.entity.task;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import com.workflow.workflow.integration.git.github.entity.GitHubIntegration;
import com.workflow.workflow.task.Task;

@Embeddable
public class GitHubTaskKey implements Serializable {
    @Column(name = "task_id")
    long taskId;

    @Column(name = "integration_id")
    long integrationId;

    public GitHubTaskKey() {
    }

    public GitHubTaskKey(Task task, GitHubIntegration integration) {
        this.taskId = task.getId();
        this.integrationId = integration.getId();
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public long getIntegrationId() {
        return integrationId;
    }

    public void setIntegrationId(long integrationId) {
        this.integrationId = integrationId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hash = prime + Long.hashCode(taskId);
        hash = prime * hash + Long.hashCode(integrationId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        GitHubTaskKey other = (GitHubTaskKey) obj;
        return taskId == other.taskId && integrationId == other.integrationId;
    }
}
