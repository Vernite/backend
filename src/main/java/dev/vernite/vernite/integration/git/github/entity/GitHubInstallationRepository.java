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

package dev.vernite.vernite.integration.git.github.entity;

import java.util.List;
import java.util.Optional;

import dev.vernite.vernite.user.User;
import dev.vernite.vernite.utils.NotFoundRepository;

public interface GitHubInstallationRepository extends NotFoundRepository<GitHubInstallation, Long> {
    /**
     * This method finds all associated GitHub installations for given user.
     * 
     * @param user user which GitHub installations will be returned;
     * @return List with all users GitHub installations.
     */
    List<GitHubInstallation> findByUser(User user);

    /**
     * This method finds GitHub installation by given user that is not suspended.
     * 
     * @param user which GitHub installation will be returned.
     * @return List with GitHub installations.
     */
    List<GitHubInstallation> findByUserAndSuspendedFalse(User user);

    /**
     * This method find installation for given user with given id.
     * 
     * @param id   of installation to find.
     * @param user which installation will be returned.
     * @return Installation with given id and user.
     */
    Optional<GitHubInstallation> findByIdAndUser(long id, User user);

    /**
     * This method finds installation by installation id.
     * 
     * @param installationId of installation to find.
     * @return Installation with given installation id.
     */
    Optional<GitHubInstallation> findByInstallationId(long installationId);

    /**
     * This method finds installation by GitHub username.
     * 
     * @param gitHubUsername GitHub username.
     * @return Installation with for given username.
     */
    Optional<GitHubInstallation> findByGitHubUsername(String gitHubUsername);
}
