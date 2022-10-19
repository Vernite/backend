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
