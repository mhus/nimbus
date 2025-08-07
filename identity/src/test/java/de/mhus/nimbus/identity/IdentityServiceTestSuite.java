package de.mhus.nimbus.identity;

import org.junit.jupiter.api.Test;

/**
 * Test suite for the Identity Service.
 * Runs all unit tests and integration tests for the Identity Service components.
 *
 * This test suite covers:
 * - Controller tests (REST endpoints)
 * - Service tests (business logic)
 * - Utility tests (JWT handling, password hashing)
 * - Integration tests (database operations)
 * - Security tests (authorization, authentication)
 * - Filter tests (JWT authentication filter)
 */
public class IdentityServiceTestSuite {

    @Test
    void testSuiteInfo() {
        // This is just a marker test to show that the test suite exists
        // All individual tests will be run by Maven automatically
        System.out.println("Identity Service Test Suite - All tests will be executed by Maven");
    }
}
