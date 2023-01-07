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
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import dev.vernite.vernite.integration.git.github.api.model.OauthToken;
import dev.vernite.vernite.integration.git.github.api.model.Repositories;
import dev.vernite.vernite.integration.git.github.api.model.request.OauthRefreshTokenRequest;
import dev.vernite.vernite.integration.git.github.api.model.request.OauthTokenRequest;
import dev.vernite.vernite.integration.git.github.api.model.AppToken;
import dev.vernite.vernite.integration.git.github.api.model.BranchName;
import dev.vernite.vernite.integration.git.github.api.model.GitHubComment;
import dev.vernite.vernite.integration.git.github.api.model.GitHubIssue;
import dev.vernite.vernite.integration.git.github.api.model.GitHubPullRequest;
import dev.vernite.vernite.integration.git.github.api.model.GitHubRelease;
import dev.vernite.vernite.integration.git.github.api.model.GitHubUser;
import dev.vernite.vernite.integration.git.github.api.model.Installations;
import dev.vernite.vernite.integration.git.github.api.model.MergeResponse;
import reactor.core.publisher.Flux;
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

    /**
     * Get the repository issues.
     * 
     * @param token installation access token
     * @param owner owner of repository
     * @param name  name of repository
     * @return the repository issues
     */
    @GetExchange("/repos/{owner}/{name}/issues")
    Flux<GitHubIssue> getRepositoryIssues(@RequestHeader("Authorization") String token, @PathVariable String owner,
            @PathVariable String name);

    /**
     * Get the repository issue.
     * 
     * @param token       installation access token
     * @param owner       owner of repository
     * @param name        name of repository
     * @param issueNumber issue number
     * @return the repository issue
     */
    @GetExchange("/repos/{owner}/{name}/issues/{issueNumber}")
    Mono<GitHubIssue> getRepositoryIssue(@RequestHeader("Authorization") String token, @PathVariable String owner,
            @PathVariable String name, @PathVariable long issueNumber);

    /**
     * Create a repository issue.
     * 
     * @param token installation access token
     * @param owner owner of repository
     * @param name  name of repository
     * @param body  issue body
     * @return the created issue
     */
    @PostExchange("/repos/{owner}/{name}/issues")
    Mono<GitHubIssue> createRepositoryIssue(@RequestHeader("Authorization") String token, @PathVariable String owner,
            @PathVariable String name, @RequestBody GitHubIssue body);

    /**
     * Get the repository pull requests.
     * 
     * @param token installation access token
     * @param owner owner of repository
     * @param name  name of repository
     * @return the repository pull requests
     */
    @GetExchange("/repos/{owner}/{name}/pulls")
    Flux<GitHubPullRequest> getRepositoryPullRequests(@RequestHeader("Authorization") String token,
            @PathVariable String owner, @PathVariable String name);

    /**
     * Get the repository pull request.
     * 
     * @param token             installation access token
     * @param owner             owner of repository
     * @param name              name of repository
     * @param pullRequestNumber pull request number
     * @return the repository pull request
     */
    @GetExchange("/repos/{owner}/{name}/pulls/{pullRequestNumber}")
    Mono<GitHubPullRequest> getRepositoryPullRequest(@RequestHeader("Authorization") String token,
            @PathVariable String owner, @PathVariable String name, @PathVariable long pullRequestNumber);

    /**
     * Get the repository collaborators.
     * 
     * @param token installation access token
     * @param owner owner of repository
     * @param name  name of repository
     * @return the repository collaborators
     */
    @GetExchange("/repos/{owner}/{name}/collaborators")
    Flux<GitHubUser> getRepositoryCollaborators(@RequestHeader("Authorization") String token,
            @PathVariable String owner, @PathVariable String name);

    /**
     * Patch a repository issue.
     * 
     * @param token installation access token
     * @param owner owner of repository
     * @param name  name of repository
     * @param id    id of issue
     * @param body  issue body
     * @return the patched issue
     */
    @PatchExchange("/repos/{owner}/{name}/issues/{id}")
    Mono<GitHubIssue> patchRepositoryIssue(@RequestHeader("Authorization") String token, @PathVariable String owner,
            @PathVariable String name, @PathVariable long id, @RequestBody GitHubIssue body);

    /**
     * Patch a repository pull request.
     * 
     * @param token installation access token
     * @param owner owner of repository
     * @param name  name of repository
     * @param id    id of issue
     * @param body  pull request body
     * @return the patched pull request
     */
    @PatchExchange("/repos/{owner}/{name}/pulls/{id}")
    Mono<GitHubPullRequest> patchRepositoryPullRequest(@RequestHeader("Authorization") String token,
            @PathVariable String owner, @PathVariable String name, @PathVariable long id,
            @RequestBody GitHubPullRequest body);

    /**
     * Merge a pull request.
     * 
     * @param token installation access token
     * @param owner owner of repository
     * @param name  name of repository
     * @param id    id of pull request
     * @return the merge response
     */
    @PutExchange("/repos/{owner}/{name}/pulls/{id}/merge")
    Mono<MergeResponse> mergePullRequest(@RequestHeader("Authorization") String token, @PathVariable String owner,
            @PathVariable String name, @PathVariable long id);

    /**
     * Get the repository branches.
     * 
     * @param token installation access token
     * @param owner owner of repository
     * @param name  name of repository
     * @return the repository branches
     */
    @GetExchange("/repos/{owner}/{name}/branches")
    Flux<BranchName> getRepositoryBranches(@RequestHeader("Authorization") String token, @PathVariable String owner,
            @PathVariable String name);

    /**
     * Create a repository release.
     * 
     * @param token   installation access token
     * @param owner   owner of repository
     * @param name    name of repository
     * @param release release body
     * @return the created release
     */
    @PostExchange("/repos/{owner}/{name}/releases")
    Mono<GitHubRelease> createRepositoryRelease(@RequestHeader("Authorization") String token,
            @PathVariable String owner, @PathVariable String name, @RequestBody GitHubRelease release);

    /**
     * Create a repository issue comment.
     * 
     * @param token       installation access token
     * @param owner       owner of repository
     * @param name        name of repository
     * @param issueNumber issue number
     * @param body        comment body
     * @return the created comment
     */
    @PostExchange("/repos/{owner}/{name}/issues/{issueNumber}/comments")
    Mono<GitHubComment> createIssueComment(@RequestHeader("Authorization") String token, @PathVariable String owner,
            @PathVariable String name, @PathVariable long issueNumber, @RequestBody GitHubComment body);

    /**
     * Get the repository issue comments.
     * 
     * @param token       installation access token
     * @param owner       owner of repository
     * @param name        name of repository
     * @param issueNumber issue number
     * @return the repository issue comments
     */
    @GetExchange("/repos/{owner}/{name}/issues/{issueNumber}/comments")
    Flux<GitHubComment> getIssueComments(@RequestHeader("Authorization") String token, @PathVariable String owner,
            @PathVariable String name, @PathVariable long issueNumber);

    /**
     * Patch a repository issue comment.
     * 
     * @param token       installation access token
     * @param owner       owner of repository
     * @param name        name of repository
     * @param commentId   comment id
     * @param body        comment body
     * @return the patched comment
     */
    @PatchExchange("/repos/{owner}/{name}/issues/comments/{commentId}")
    Mono<GitHubComment> patchIssueComment(@RequestHeader("Authorization") String token, @PathVariable String owner,
            @PathVariable String name, @PathVariable long commentId, @RequestBody GitHubComment body);

}
