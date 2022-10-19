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

package com.workflow.workflow.integration.git.github.entity;

import java.util.List;
import java.util.Optional;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.utils.SoftDeleteRepository;

public interface GitHubIntegrationRepository extends SoftDeleteRepository<GitHubIntegration, Long> {
    /**
     * This method finds integration with GitHub for given project.
     * 
     * @param project - project for which integration will be found.
     * @return Integration for given project.
     */
    Optional<GitHubIntegration> findByProjectAndActiveNull(Project project);

    /**
     * This method finds all integrations for given GitHub account.
     * 
     * @param installation - installation for which integrations will be found.
     * @return Integrations associated with given installation.
     */
    List<GitHubIntegration> findByInstallation(GitHubInstallation installation);

    /**
     * This method finds all integrations for repository full name.
     * 
     * @param respositoryFullName - full name of GitHub repostory to which
     *                            integrations will be returned.
     * @return Integrations associated with repository with given name.
     */
    List<GitHubIntegration> findByRepositoryFullName(String respositoryFullName);
}
