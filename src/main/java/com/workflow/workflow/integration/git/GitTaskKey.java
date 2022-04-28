package com.workflow.workflow.integration.git;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import com.workflow.workflow.task.Task;

@Embeddable
public class GitTaskKey implements Serializable {
    @Column(name = "task_id") Long taskId;
    @Column(name = "integration_id") Long integrationId;

    public GitTaskKey() {}

    public GitTaskKey(Task task, GitIntegration integration) {
        this.taskId = task.getId();
        this.integrationId = integration.getId();
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getIntegrationId() {
        return integrationId;
    }

    public void setIntegrationId(Long integrationId) {
        this.integrationId = integrationId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((taskId == null) ? 0 : taskId.hashCode());
        result = prime * result + ((integrationId == null) ? 0 : integrationId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GitTaskKey other = (GitTaskKey) obj;
        if (taskId == null) {
            if (other.taskId != null)
                return false;
        } else if (!taskId.equals(other.taskId))
            return false;
        if (integrationId == null) {
            if (other.integrationId != null)
                return false;
        } else if (!integrationId.equals(other.integrationId))
            return false;
        return true;
    }
}
