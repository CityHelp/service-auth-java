package com.crudzaso.cityhelp.auth.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests.
 * Uses Testcontainers for PostgreSQL and Redis with real infrastructure scenarios.
 *
 * <p>This class provides:
 * - Real PostgreSQL database in Docker container
 * - Real Redis cache in Docker container
 * - Dynamic property configuration for Testcontainers
 * - Transaction rollback after each test
 * - Flyway migrations enabled for proper schema setup</p>
 *
 * <p>Extend this class for all integration tests to ensure consistency
 * and realistic infrastructure testing scenarios.</p>
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
     * Using static initialization to ensure container is started before property configuration.
     */
    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("cityhelp_auth_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true)
            .withLabel("app", "cityhelp-auth-test");

    /**
     * Redis container with official Redis 7 Alpine image.
     * Cache is created fresh for each test class.
     * Used for rate limiting and session management in integration tests.
     */
    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true)
            .withLabel("app", "cityhelp-auth-test-redis");
    
    /**
     * Configures dynamic properties for Testcontainers.
     * Spring Boot will use these properties to connect to container infrastructure.
     *
     * @param registry dynamic property registry
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Ensure containers are started before getting properties
        if (!postgres.isRunning()) {
            postgres.start();
        }
        if (!redis.isRunning()) {
            redis.start();
        }

        // PostgreSQL properties
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        // Configure Flyway to use the Testcontainers database
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");

        // Redis properties
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }
    
    /**
     * Setup method called once before all tests in the class.
     * Container is already started in static initializer.
     */
    @BeforeAll
    static void setUpClass() {
        // Container is already started in static initializer
        // Additional setup can be added here if needed
    }

    /**
     * Cleanup method called once after all tests in the class.
     * Closes containers to prevent resource leaks.
     */
    @AfterAll
    static void tearDownClass() {
        if (postgres != null && postgres.isRunning()) {
            postgres.close();
        }
        if (redis != null && redis.isRunning()) {
            redis.close();
        }
    }
}