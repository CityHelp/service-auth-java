package com.crudzaso.cityhelp.auth.unit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;
import org.mockito.InOrder;


/**
 * Base class for all unit tests in CityHelp Auth Service.
 * Provides common setup and teardown functionality using JUnit 5 and Mockito.
 *
 * This class replaces JUnit 4's MockitoJUnitRunner functionality in JUnit 5 environment
 * and serves as a foundation for all application layer unit tests.
 *
 * Stack: JUnit 5, Mockito 5.x, Spring Boot Test
 */
public abstract class BaseUnitTest {

    /**
     * Initialize mocks before each test method.
     * This replaces @RunWith(MockitoJUnitRunner.class) from JUnit 4
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Clean up after each test method.
     * Currently no cleanup needed, but available for future use.
     */
    @AfterEach
    void tearDown() {
        // Cleanup can be added here if needed in the future
    }

    /**
     * Creates an InOrder verifier for ordered method call verification.
     * Utility method to maintain consistency across test classes.
     *
     * @param mocks Mock objects to verify in order
     * @return InOrder verifier instance
     */
    protected InOrder inOrder(Object... mocks) {
        return inOrder(mocks);
    }
}