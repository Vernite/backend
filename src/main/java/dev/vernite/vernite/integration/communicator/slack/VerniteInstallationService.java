package dev.vernite.vernite.integration.communicator.slack;


import com.slack.api.bolt.model.Bot;
import com.slack.api.bolt.model.Installer;
import com.slack.api.bolt.model.builtin.DefaultInstaller;
import com.slack.api.bolt.service.InstallationService;

import dev.vernite.vernite.integration.communicator.slack.entity.SlackInstallation;
import dev.vernite.vernite.integration.communicator.slack.entity.SlackInstallationRepository;

public class VerniteInstallationService implements InstallationService {

    boolean isHistoricalDataEnabled = false;

    private SlackInstallationRepository repository;

    public VerniteInstallationService(SlackInstallationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void deleteBot(Bot arg0) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteInstaller(Installer installer) throws Exception {
        repository.findByToken(installer.getInstallerUserAccessToken()).ifPresent(repository::delete);
    }

    @Override
    public Bot findBot(String arg0, String arg1) {
        return null;
    }

    @Override
    public Installer findInstaller(String enterpriseId, String teamId, String userId) {
        SlackInstallation installation = repository.findByTeamIdAndInstallerUserId(teamId, userId).orElse(null);
        if (installation == null) {
            return null;
        } else {
            return DefaultInstaller.builder()
                    .installerUserAccessToken(installation.getToken())
                    .installedAt(installation.getInstalledAt())
                    .installerUserId(userId)
                    .teamId(teamId)
                    .appId("A04BU7X5J69")
                    .build();
        }
    }

    @Override
    public boolean isHistoricalDataEnabled() {
        return isHistoricalDataEnabled;
    }

    @Override
    public void saveInstallerAndBot(Installer installer) throws Exception {
        SlackInstallation installation = new SlackInstallation(
                installer.getInstallerUserAccessToken(),
                installer.getInstalledAt(),
                installer.getInstallerUserId(),
                installer.getTeamId());
        repository.save(installation);
    }

    @Override
    public void setHistoricalDataEnabled(boolean isHistoricalDataEnabled) {
        this.isHistoricalDataEnabled = isHistoricalDataEnabled;
    }
    
}
