package com.workflow.workflow;

import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;

@SpringBootApplication
@OpenAPIDefinition(servers = @Server(url = "/api"))
public class WorkflowApplication {
	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(WorkflowApplication.class);
		Properties properties = new Properties();
		properties.setProperty("spring.datasource.password", Tokens.TOKENS.mysqlPassword);
		properties.setProperty("spring.mail.password", Tokens.TOKENS.mailPassword);
		app.setDefaultProperties(properties);
		app.run(args);
	}
}
