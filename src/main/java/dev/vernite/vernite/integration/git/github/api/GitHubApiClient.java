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

package dev.vernite.vernite.integration.git.github.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import dev.vernite.vernite.integration.git.github.api.model.OauthToken;
import dev.vernite.vernite.integration.git.github.api.model.Repositories;
import dev.vernite.vernite.integration.git.github.api.model.request.OauthRefreshTokenRequest;
import dev.vernite.vernite.integration.git.github.api.model.request.OauthTokenRequest;
import dev.vernite.vernite.integration.git.github.api.model.AppToken;
import dev.vernite.vernite.integration.git.github.api.model.GitHubUser;
import dev.vernite.vernite.integration.git.github.api.model.Installations;
import reactor.core.publisher.Mono;

/**
 * The GitHub API service. This is the entry point for all GitHub API calls.
 */
@HttpExchange(accept = "application/vnd.github.v3+json")
public interface GitHubApiClient {

    /**
     * Get the OAuth access token.
     * 
     * @param oauthTokenRequest the OAuth token request
     * @return the access token
     */
    @PostExchange("https://github.com/login/oauth/access_token")
    Mono<OauthToken> createOauthAccessToken(@RequestBody OauthTokenRequest oauthTokenRequest);

    /**
     * Refresh the OAuth access token.
     * 
     * @param oauthRefreshTokenRequest the OAuth refresh token request
     * @return the refreshed access token
     */
    @PostExchange("https://github.com/login/oauth/access_token")
    Mono<OauthToken> refreshOauthAccessToken(@RequestBody OauthRefreshTokenRequest oauthRefreshTokenRequest);

    /**
     * Get the authenticated user.
     * 
     * @param authorization the authorization header
     * @return the authenticated user
     */
    @GetExchange("/user")
    Mono<GitHubUser> getAuthenticatedUser(@RequestHeader("Authorization") String authorization);

    /**
     * Get the authenticated user's installations.
     * 
     * @param authorization the authorization header
     * @return the authenticated user's installations
     */
    @GetExchange("/user/installations")
    Mono<Installations> getUserInstallations(@RequestHeader("Authorization") String authorization);

    /**
     * Create app access token.
     * 
     * @param jwt JSON web token
     * @param id  id of installation
     * @return the access token
     */
    @PostExchange("/app/installations/{id}/access_tokens")
    Mono<AppToken> createInstallationAccessToken(@RequestHeader("Authorization") String jwt, @PathVariable long id);

    /**
     * Get the installation repositories.
     * 
     * @param token installation access token
     * @return the installations repositories
     */
    @GetExchange("/installation/repositories")
    Mono<Repositories> getInstallationRepositories(@RequestHeader("Authorization") String token);

}
