package com.crudzaso.cityhelp.auth.infrastructure.security;

import com.crudzaso.cityhelp.auth.application.exception.TooManyRequestsException;
import com.crudzaso.cityhelp.auth.infrastructure.dto.LoginRequest;
import com.crudzaso.cityhelp.auth.infrastructure.dto.RegisterRequest;
import com.crudzaso.cityhelp.auth.infrastructure.dto.VerifyEmailRequest;
import com.crudzaso.cityhelp.auth.infrastructure.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AOP Aspect for intercepting methods annotated with @RateLimited.
 * Enforces rate limiting by checking Redis counters before allowing method execution.
 *
 * How it works:
 * 1. Intercepts methods with @RateLimited annotation
 * 2. Extracts identifier (email from request body or IP address)
 * 3. Checks if rate limit is exceeded using RateLimitService
 * 4. Throws TooManyRequestsException if limit exceeded
 * 5. Allows method execution if within limits
 *
 * Identifier Priority:
 * - Email from request body (LoginRequest, RegisterRequest, VerifyEmailRequest)
 * - IP address from HTTP headers (X-Forwarded-For, X-Real-IP)
 * - Remote address from HttpServletRequest
 *
 * @author CityHelp Team
 * @since 1.0.0
 * @see RateLimited
 * @see RateLimitService
 * @see TooManyRequestsException
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);

    private final RateLimitService rateLimitService;

    /**
     * Constructor injection for RateLimitService.
     *
     * @param rateLimitService the rate limit service
     */
    public RateLimitAspect(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    /**
     * Around advice that intercepts methods annotated with @RateLimited.
     * Checks rate limit before allowing method execution.
     *
     * @param joinPoint the join point representing the intercepted method
     * @param rateLimited the @RateLimited annotation with configuration
     * @return the result of the method execution if allowed
     * @throws Throwable if method execution fails
     * @throws TooManyRequestsException if rate limit is exceeded
     */
    @Around("@annotation(rateLimited)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        // Extract HTTP request from RequestContextHolder
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            logger.warn("No request attributes found. Skipping rate limit check.");
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();

        // Extract identifier (email from request body or IP address)
        String identifier = extractIdentifier(joinPoint, request);

        // Build rate limit key
        String keyPrefix = rateLimited.key();
        int limit = rateLimited.limit();
        long windowSeconds = rateLimited.windowSeconds();

        logger.debug("Checking rate limit for: key={}, identifier={}, limit={}, window={}s",
                keyPrefix, identifier, limit, windowSeconds);

        // Check if request is allowed
        boolean allowed = rateLimitService.isAllowed(keyPrefix, identifier, limit, windowSeconds);

        if (!allowed) {
            String errorMessage = String.format(
                    "Rate limit exceeded for %s. Please try again in %d minutes.",
                    keyPrefix, windowSeconds / 60
            );
            logger.warn("Rate limit exceeded: key={}, identifier={}, limit={}", keyPrefix, identifier, limit);
            throw new TooManyRequestsException(errorMessage);
        }

        // Allow method execution
        logger.debug("Rate limit check passed. Proceeding with method execution.");
        return joinPoint.proceed();
    }

    /**
     * Extracts an identifier for rate limiting from the method arguments or HTTP request.
     * Priority:
     * 1. Email from LoginRequest, RegisterRequest, or VerifyEmailRequest
     * 2. IP address from HttpServletRequest
     *
     * @param joinPoint the join point containing method arguments
     * @param request the HTTP servlet request
     * @return the extracted identifier (email or IP)
     */
    private String extractIdentifier(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        // Check method arguments for request DTOs
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof LoginRequest) {
                String email = ((LoginRequest) arg).getEmail();
                if (email != null && !email.isEmpty()) {
                    logger.debug("Extracted identifier from LoginRequest: {}", email);
                    return email;
                }
            } else if (arg instanceof RegisterRequest) {
                String email = ((RegisterRequest) arg).getEmail();
                if (email != null && !email.isEmpty()) {
                    logger.debug("Extracted identifier from RegisterRequest: {}", email);
                    return email;
                }
            } else if (arg instanceof VerifyEmailRequest) {
                String email = ((VerifyEmailRequest) arg).getEmail();
                if (email != null && !email.isEmpty()) {
                    logger.debug("Extracted identifier from VerifyEmailRequest: {}", email);
                    return email;
                }
            }
        }

        // Fallback to IP address
        String ipAddress = rateLimitService.extractIdentifier(request);
        logger.debug("Using IP address as identifier: {}", ipAddress);
        return ipAddress;
    }
}
