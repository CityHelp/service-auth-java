package com.crudzaso.cityhelp.auth.infrastructure.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Centralized metrics service for CityHelp Auth Service.
 * Provides helper methods to record custom metrics for authentication, password management,
 * email verification, and token operations using Micrometer.
 *
 * Metrics are exposed via /actuator/prometheus endpoint for Prometheus scraping.
 *
 * Architecture:
 * - Dependency Injection: MeterRegistry injected by Spring
 * - Counters: Track cumulative events (login attempts, registrations, etc.)
 * - Timers: Measure operation duration (login time, verification time, etc.)
 * - Gauges: Track point-in-time values (active sessions, etc.)
 *
 * Metric Naming Convention:
 * - Prefix: auth.* (for authentication metrics)
 * - Format: auth.{operation}.{metric_type}.{event}
 * - Tags: operation, status, endpoint for better aggregation
 *
 * Example usage in controllers/services:
 * - metricsService.recordLoginAttempt(true, "manual");
 * - Timer.Sample sample = metricsService.startTimer();
 * - metricsService.stopTimer(sample, "email.verification", "success");
 */
@Slf4j
@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;

    // Counter metrics
    private final Counter loginAttemptsCounter;
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter accountLockoutCounter;

    private final Counter emailVerificationCodessentCounter;
    private final Counter emailVerificationSuccessCounter;
    private final Counter emailVerificationFailureCounter;
    private final Counter emailVerificationResendCounter;

    private final Counter passwordResetRequestsCounter;
    private final Counter passwordResetSuccessCounter;
    private final Counter passwordResetFailureCounter;
    private final Counter passwordChangeSuccessCounter;
    private final Counter passwordChangeFailureCounter;

    private final Counter tokenRefreshAttemptsCounter;
    private final Counter tokenRefreshSuccessCounter;
    private final Counter tokenRefreshFailureCounter;
    private final Counter logoutCounter;

    private final Counter userRegistrationCounter;
    private final Counter userAccountDeletionCounter;

    private final Counter rateLimitExceededCounter;
    private final Counter oauthLoginSuccessCounter;

    // Timer metrics
    private final Timer loginDurationTimer;
    private final Timer emailVerificationDurationTimer;
    private final Timer passwordResetDurationTimer;
    private final Timer tokenRefreshDurationTimer;
    private final Timer registrationDurationTimer;

    // Gauge metrics
    private final AtomicLong failedLoginAttemptsGauge = new AtomicLong(0);

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize authentication counters
        this.loginAttemptsCounter = Counter.builder("auth.login.attempts.total")
                .description("Total login attempts (including successful and failed)")
                .tag("operation", "login")
                .register(meterRegistry);

        this.loginSuccessCounter = Counter.builder("auth.login.success.total")
                .description("Total successful login attempts")
                .tag("operation", "login")
                .tag("status", "success")
                .register(meterRegistry);

        this.loginFailureCounter = Counter.builder("auth.login.failure.total")
                .description("Total failed login attempts")
                .tag("operation", "login")
                .tag("status", "failure")
                .register(meterRegistry);

        this.accountLockoutCounter = Counter.builder("auth.account.lockout.total")
                .description("Total account lockouts due to failed login attempts")
                .tag("operation", "login")
                .tag("event", "lockout")
                .register(meterRegistry);

        // Initialize email verification counters
        this.emailVerificationCodessentCounter = Counter.builder("auth.email.verification.codes.sent.total")
                .description("Total email verification codes sent")
                .tag("operation", "email_verification")
                .tag("event", "code_sent")
                .register(meterRegistry);

        this.emailVerificationSuccessCounter = Counter.builder("auth.email.verification.success.total")
                .description("Total successful email verifications")
                .tag("operation", "email_verification")
                .tag("status", "success")
                .register(meterRegistry);

        this.emailVerificationFailureCounter = Counter.builder("auth.email.verification.failure.total")
                .description("Total failed email verification attempts")
                .tag("operation", "email_verification")
                .tag("status", "failure")
                .register(meterRegistry);

        this.emailVerificationResendCounter = Counter.builder("auth.email.verification.resend.total")
                .description("Total email verification code resends")
                .tag("operation", "email_verification")
                .tag("event", "resend")
                .register(meterRegistry);

        // Initialize password management counters
        this.passwordResetRequestsCounter = Counter.builder("auth.password.reset.requests.total")
                .description("Total password reset requests")
                .tag("operation", "password_reset")
                .tag("event", "request")
                .register(meterRegistry);

        this.passwordResetSuccessCounter = Counter.builder("auth.password.reset.success.total")
                .description("Total successful password resets")
                .tag("operation", "password_reset")
                .tag("status", "success")
                .register(meterRegistry);

        this.passwordResetFailureCounter = Counter.builder("auth.password.reset.failure.total")
                .description("Total failed password reset attempts")
                .tag("operation", "password_reset")
                .tag("status", "failure")
                .register(meterRegistry);

        this.passwordChangeSuccessCounter = Counter.builder("auth.password.change.success.total")
                .description("Total successful password changes")
                .tag("operation", "password_change")
                .tag("status", "success")
                .register(meterRegistry);

        this.passwordChangeFailureCounter = Counter.builder("auth.password.change.failure.total")
                .description("Total failed password change attempts")
                .tag("operation", "password_change")
                .tag("status", "failure")
                .register(meterRegistry);

        // Initialize token management counters
        this.tokenRefreshAttemptsCounter = Counter.builder("auth.token.refresh.attempts.total")
                .description("Total token refresh attempts")
                .tag("operation", "token_refresh")
                .register(meterRegistry);

        this.tokenRefreshSuccessCounter = Counter.builder("auth.token.refresh.success.total")
                .description("Total successful token refreshes")
                .tag("operation", "token_refresh")
                .tag("status", "success")
                .register(meterRegistry);

        this.tokenRefreshFailureCounter = Counter.builder("auth.token.refresh.failure.total")
                .description("Total failed token refresh attempts")
                .tag("operation", "token_refresh")
                .tag("status", "failure")
                .register(meterRegistry);

        this.logoutCounter = Counter.builder("auth.logout.total")
                .description("Total logout operations")
                .tag("operation", "logout")
                .register(meterRegistry);

        // Initialize user management counters
        this.userRegistrationCounter = Counter.builder("auth.users.registered.total")
                .description("Total user registrations")
                .tag("operation", "registration")
                .register(meterRegistry);

        this.userAccountDeletionCounter = Counter.builder("auth.users.deleted.total")
                .description("Total user account deletions")
                .tag("operation", "deletion")
                .register(meterRegistry);

        // Initialize other counters
        this.rateLimitExceededCounter = Counter.builder("auth.rate.limit.exceeded.total")
                .description("Total rate limit exceeded events")
                .tag("operation", "rate_limiting")
                .register(meterRegistry);

        this.oauthLoginSuccessCounter = Counter.builder("auth.oauth.login.success.total")
                .description("Total successful OAuth2 logins (Google, etc.)")
                .tag("operation", "oauth_login")
                .tag("status", "success")
                .register(meterRegistry);

        // Initialize timer metrics
        this.loginDurationTimer = Timer.builder("auth.login.duration")
                .description("Time taken to complete login operation")
                .tag("operation", "login")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.emailVerificationDurationTimer = Timer.builder("auth.email.verification.duration")
                .description("Time taken to complete email verification")
                .tag("operation", "email_verification")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.passwordResetDurationTimer = Timer.builder("auth.password.reset.duration")
                .description("Time taken to complete password reset")
                .tag("operation", "password_reset")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.tokenRefreshDurationTimer = Timer.builder("auth.token.refresh.duration")
                .description("Time taken to complete token refresh")
                .tag("operation", "token_refresh")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.registrationDurationTimer = Timer.builder("auth.registration.duration")
                .description("Time taken to complete user registration")
                .tag("operation", "registration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    // ============= Login Metrics =============

    public void recordLoginAttempt() {
        loginAttemptsCounter.increment();
    }

    public void recordLoginSuccess() {
        loginSuccessCounter.increment();
    }

    public void recordLoginFailure() {
        loginFailureCounter.increment();
    }

    public void recordAccountLockout() {
        accountLockoutCounter.increment();
    }

    public Timer.Sample startLoginTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordLoginDuration(Timer.Sample sample) {
        sample.stop(loginDurationTimer);
    }

    // ============= Email Verification Metrics =============

    public void recordEmailVerificationCodeSent() {
        emailVerificationCodessentCounter.increment();
    }

    public void recordEmailVerificationSuccess() {
        emailVerificationSuccessCounter.increment();
    }

    public void recordEmailVerificationFailure() {
        emailVerificationFailureCounter.increment();
    }

    public void recordEmailVerificationResend() {
        emailVerificationResendCounter.increment();
    }

    public Timer.Sample startEmailVerificationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordEmailVerificationDuration(Timer.Sample sample) {
        sample.stop(emailVerificationDurationTimer);
    }

    // ============= Password Management Metrics =============

    public void recordPasswordResetRequest() {
        passwordResetRequestsCounter.increment();
    }

    public void recordPasswordResetSuccess() {
        passwordResetSuccessCounter.increment();
    }

    public void recordPasswordResetFailure() {
        passwordResetFailureCounter.increment();
    }

    public void recordPasswordChangeSuccess() {
        passwordChangeSuccessCounter.increment();
    }

    public void recordPasswordChangeFailure() {
        passwordChangeFailureCounter.increment();
    }

    public Timer.Sample startPasswordResetTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordPasswordResetDuration(Timer.Sample sample) {
        sample.stop(passwordResetDurationTimer);
    }

    // ============= Token Management Metrics =============

    public void recordTokenRefreshAttempt() {
        tokenRefreshAttemptsCounter.increment();
    }

    public void recordTokenRefreshSuccess() {
        tokenRefreshSuccessCounter.increment();
    }

    public void recordTokenRefreshFailure() {
        tokenRefreshFailureCounter.increment();
    }

    public void recordLogout() {
        logoutCounter.increment();
    }

    public Timer.Sample startTokenRefreshTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordTokenRefreshDuration(Timer.Sample sample) {
        sample.stop(tokenRefreshDurationTimer);
    }

    // ============= User Management Metrics =============

    public void recordUserRegistration() {
        userRegistrationCounter.increment();
    }

    public void recordUserAccountDeletion() {
        userAccountDeletionCounter.increment();
    }

    public Timer.Sample startRegistrationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordRegistrationDuration(Timer.Sample sample) {
        sample.stop(registrationDurationTimer);
    }

    // ============= Rate Limiting Metrics =============

    public void recordRateLimitExceeded() {
        rateLimitExceededCounter.increment();
    }

    // ============= OAuth Metrics =============

    public void recordOAuthLoginSuccess(String provider) {
        Counter.builder("auth.oauth.login.success.total")
                .description("Successful OAuth2 logins by provider")
                .tag("operation", "oauth_login")
                .tag("status", "success")
                .tag("provider", provider)
                .register(meterRegistry)
                .increment();

        // Also increment the generic counter
        oauthLoginSuccessCounter.increment();
    }

    // ============= Gauge Metrics (Point-in-time values) =============

    /**
     * Record the number of failed login attempts.
     * This is used to track the current state of failed attempts.
     */
    public void updateFailedLoginAttempts(long count) {
        failedLoginAttemptsGauge.set(count);
    }

    /**
     * Create a gauge for tracking active sessions or any other point-in-time value.
     * Example: Track pending password reset tokens
     */
    public void registerCustomGauge(String metricName, String description,
                                   java.util.function.Supplier<Number> valueSupplier,
                                   String... tags) {
        io.micrometer.core.instrument.Gauge.builder(metricName, valueSupplier)
                .description(description)
                .tags(Tags.of(tags))
                .register(meterRegistry);
    }

    /**
     * Record a custom counter increment for flexible metric recording.
     */
    public void recordCustomCounter(String metricName, String description, double value, String... tags) {
        Counter.builder(metricName)
                .description(description)
                .tags(Tags.of(tags))
                .register(meterRegistry)
                .increment(value);
    }

    /**
     * Record a generic timer duration for flexible metric recording.
     */
    public void recordCustomTimer(String metricName, String description,
                                 long duration, java.util.concurrent.TimeUnit unit, String... tags) {
        Timer.builder(metricName)
                .description(description)
                .tags(Tags.of(tags))
                .register(meterRegistry)
                .record(duration, unit);
    }
}
