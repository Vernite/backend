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

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.workflow.workflow.integration.git.github.entity.GitHubIntegration;
import com.workflow.workflow.task.Task;

@NoRepositoryBean
public interface GitHubTaskRepository<T, K> extends JpaRepository<T, K> {
    /**
     * This method finds GitHub issue / pull request connection for task.
     * 
     * @param task which connection is looked for.
     * @return Optional with connection to GitHub issue / pull request; empty when
     *         there is not any.
     */
    Optional<T> findByTask(Task task);

    /**
     * This method finds GitHub issues connection for integration.
     * 
     * @param integration which connection is looked for.
     * @return List of all task associeted with integration.
     */
    List<T> findByGitHubIntegration(GitHubIntegration integration);

    /**
     * This method finds GitHub issue connections for integration and issue id.
     * 
     * @param issueId     id of github issue.
     * @param integration integration with github.
     * @return List with connections between issue and task.
     */
    List<T> findByIssueIdAndGitHubIntegration(long issueId, GitHubIntegration integration);
}
