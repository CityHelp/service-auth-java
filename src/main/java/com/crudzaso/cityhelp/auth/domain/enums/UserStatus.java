package com.crudzaso.cityhelp.auth.domain.enums;

/**
 * User account statuses for the CityHelp authentication system.
 * Follows English naming convention for technical code.
 * Status flow: pending_verification → active → deleted/suspended
 */
public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    DELETED,
    SUSPENDED
}