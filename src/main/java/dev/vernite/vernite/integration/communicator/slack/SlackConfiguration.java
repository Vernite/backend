package dev.vernite.vernite.integration.communicator.slack;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.AppConfig.AppConfigBuilder;

import dev.vernite.vernite.integration.communicator.slack.entity.SlackInstallationRepository;

@Configuration
public class SlackConfiguration {
    @Bean
    public App initSlackApp(VerniteInstallationService service, Environment env) {
        AppConfigBuilder builder = AppConfig.builder()
            .signingSecret(env.getProperty("slack.signingSecret"))
            .clientId(env.getProperty("slack.clientId"))
            .clientSecret(env.getProperty("slack.clientSecret"))
            .userScope(env.getProperty("slack.userScope"));
        return new App(builder.build()).asOAuthApp(true).service(service)
                .enableTokenRevocationHandlers();
    }

    @Bean
    public VerniteInstallationService initVerniteInstallationService(SlackInstallationRepository repository) {
        return new VerniteInstallationService(repository);
    }
}
