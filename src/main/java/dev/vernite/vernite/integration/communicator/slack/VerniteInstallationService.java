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
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHistoricalDataEnabled(boolean isHistoricalDataEnabled) {
        this.isHistoricalDataEnabled = isHistoricalDataEnabled;
    }

}
