package dev.vernite.vernite.integration.communicator.slack.entity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import dev.vernite.vernite.user.User;

public interface SlackInstallationRepository extends CrudRepository<SlackInstallation, Long> {
    Optional<SlackInstallation> findByToken(String token);

    Optional<SlackInstallation> findByTeamIdAndInstallerUserId(String teamId, String installerUserId);

    List<SlackInstallation> findByUser(User user);
}