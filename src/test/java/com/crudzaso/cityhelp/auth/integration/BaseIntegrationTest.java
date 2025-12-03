package com.crudzaso.cityhelp.auth.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests.
 * Uses Testcontainers for PostgreSQL with real database scenarios.
 * 
 * <p>This class provides:
 * - Real PostgreSQL database in Docker container
 * - Dynamic property configuration for Testcontainers
 * - Transaction rollback after each test
 * - Flyway migrations enabled for proper schema setup</p>
 * 
 * <p>Extend this class for all integration tests to ensure consistency
 * and realistic database testing scenarios.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public abstract class BaseIntegrationTest {
    
    /**
     * PostgreSQL container with official PostgreSQL 15 image.
     * Database is created fresh for each test class.
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("cityhelp_auth_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true)
            .withLabel("app", "cityhelp-auth-test");
    
    /**
     * Configures dynamic properties for Testcontainers.
     * Spring Boot will use these properties to connect to the container database.
     * 
     * @param registry dynamic property registry
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        
        // Configure Flyway to use the Testcontainers database
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
    }
    
    /**
     * Setup method called once before all tests in the class.
     * Starts the PostgreSQL container if not already running.
     */
    @BeforeAll
    static void setUpClass() {
        // Container is automatically started by @Container annotation
        // Additional setup can be added here if needed
    }
}