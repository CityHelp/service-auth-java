package com.crudzaso.cityhelp.auth.unit.infrastructure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

/**
 * Base class for infrastructure unit tests.
 * Provides common setup and teardown functionality for infrastructure components.
 * 
 * <p>This class automatically initializes Mockito annotations and manages
 * Mockito session to ensure proper cleanup between tests.</p>
 * 
 * <p>Extend this class for all infrastructure unit tests to ensure consistency
 * and proper resource management.</p>
 */
public abstract class BaseUnitTest {
    
    // Using openMocks() approach - no session needed
    
    /**
     * Setup method called before each test.
     * Initializes Mockito annotations and starts a new Mockito session.
     */
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    /**
     * Teardown method called after each test.
     * Closes Mockito session to prevent memory leaks.
     */
    @AfterEach
    void tearDown() {
        // No cleanup needed with openMocks()
    }
}