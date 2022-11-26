package dev.vernite.vernite.integration.git.github.integration;

import dev.vernite.vernite.integration.git.common.GitProjectIntegration;
import dev.vernite.vernite.integration.git.common.GitProjectIntegrationData;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Model for GitHub integration.
 */
@ToString
@EqualsAndHashCode(callSuper = true)
public class GitHubProjectIntegration extends GitProjectIntegration {

    /**
     * Default constructor for GitHub integration.
     * 
     * @param id                 integration entity id
     * @param active             whether integration is active
     * @param repositoryFullName GitHub repository full name
     */
    public GitHubProjectIntegration(long id, boolean active, String repositoryFullName) {
        super(id, "GitHub", active, new GitProjectIntegrationData("https://github.com/" + repositoryFullName));
    }

}
