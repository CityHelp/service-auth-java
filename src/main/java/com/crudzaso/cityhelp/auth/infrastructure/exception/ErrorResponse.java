package com.crudzaso.cityhelp.auth.infrastructure.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized error response structure for the CityHelp Auth Service.
 *
 * This class provides a consistent format for all error responses returned
 * by the REST API. It includes essential information about the error such as
 * timestamp, HTTP status code, error type, message, and request path.
 *
 * Features:
 * - ISO 8601 timestamp for error occurrence
 * - HTTP status code for error type
 * - Human-readable error description
 * - Detailed message for debugging
 * - Request path that caused the error
 * - Optional validation errors map for field-specific errors
 *
 * Example JSON response:
 * {
 *   "timestamp": "2025-12-04T10:30:00",
 *   "status": 400,
 *   "error": "Validation Failed",
 *   "message": "Invalid request parameters",
 *   "path": "/api/auth/register",
 *   "validationErrors": {
 *     "email": "must be a well-formed email address",
 *     "password": "must be at least 8 characters"
 *   }
 * }
 *
 * @author CityHelp Development Team
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Timestamp when the error occurred (ISO 8601 format).
     */
    private LocalDateTime timestamp;

    /**
     * HTTP status code of the error response.
     * Examples: 400 (Bad Request), 401 (Unauthorized), 404 (Not Found), 500 (Internal Server Error)
     */
    private int status;

    /**
     * Short, human-readable error type.
     * Examples: "Validation Failed", "Invalid Credentials", "Resource Not Found"
     */
    private String error;

    /**
     * Detailed error message explaining what went wrong.
     * Should be informative but not expose sensitive system details.
     */
    private String message;

    /**
     * Request path that triggered the error.
     * Helps clients identify which endpoint caused the issue.
     */
    private String path;

    /**
     * Optional map of field-specific validation errors.
     * Only included when validation fails on request DTOs.
     * Key: field name, Value: validation error message
     */
    private Map<String, String> validationErrors;
}
