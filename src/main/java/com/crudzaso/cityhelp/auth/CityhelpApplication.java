package com.crudzaso.cityhelp.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main Spring Boot application class for CityHelp Auth Service.
 *
 * @EnableJpaAuditing enables automatic auditing for entities extending AuditableEntity.
 * This allows automatic population of createdAt and updatedAt timestamps.
 */
@SpringBootApplication
@EnableJpaAuditing
public class CityhelpApplication {

	public static void main(String[] args) {
		SpringApplication.run(CityhelpApplication.class, args);
	}

}
