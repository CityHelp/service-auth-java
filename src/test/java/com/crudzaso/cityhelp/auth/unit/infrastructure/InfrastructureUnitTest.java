package com.crudzaso.cityhelp.auth.unit.infrastructure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.InOrder;

/**
 * Base class for all infrastructure unit tests in CityHelp Auth Service.
 * Provides common setup and teardown functionality using JUnit 5 and Mockito.
 *
 * This class provides the same functionality as BaseUnitTest but for infrastructure layer tests,
 * allowing proper package visibility and avoiding inheritance issues.
 *
 * Stack: JUnit 5, Mockito 5.x, Spring Boot Test
 */
public abstract class InfrastructureUnitTest {

    /**
     * Initialize mocks before each test method.
     * This replaces @RunWith(MockitoJUnitRunner.class) from JUnit 4
     */
    @BeforeEach
    protected void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Clean up after each test method.
     * Currently no cleanup needed, but available for future use.
     */
    @AfterEach
    protected void tearDown() {
        // Cleanup can be added here if needed in future
    }

    /**
     * Creates an InOrder verifier for ordered method call verification.
     * Utility method to maintain consistency across test classes.
     *
     * @param mocks Mock objects to verify in order
     * @return InOrder verifier instance
     */
    protected InOrder inOrder(Object... mocks) {
        return Mockito.inOrder(mocks);
    }
}