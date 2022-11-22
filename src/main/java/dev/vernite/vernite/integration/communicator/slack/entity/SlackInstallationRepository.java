package dev.vernite.vernite.integration.communicator.slack.entity;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface SlackInstallationRepository extends CrudRepository<SlackInstallation, Long> {
    Optional<SlackInstallation> findByToken(String token);

    Optional<SlackInstallation> findByTeamIdAndInstallerUserId(String teamId, String installerUserId);
}