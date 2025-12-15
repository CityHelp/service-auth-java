package com.crudzaso.cityhelp.auth.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for the CityHelp Auth Service.
 *
 * This class configures Swagger/OpenAPI documentation for the REST API,
 * providing interactive API documentation accessible at /swagger-ui.html.
 *
 * Key Features:
 * - Comprehensive API metadata (title, version, description, contact)
 * - JWT Bearer token authentication scheme for protected endpoints
 * - Multiple server configurations (local, dev, prod)
 * - License and terms of service information
 * - Global security requirements
 *
 * The configuration enables:
 * - Interactive API testing via Swagger UI
 * - Automatic API documentation generation
 * - Client code generation support
 * - OpenAPI 3.0 specification compliance
 *
 * @author CityHelp Development Team
 * @version 1.0
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:CityHelp Auth Service}")
    private String applicationName;

    @Value("${server.port:8001}")
    private String serverPort;

    @Value("${app.api-docs.dev-url:http://localhost:8001}")
    private String apiDocsDevUrl;

    @Value("${app.api-docs.prod-url:http://188.245.114.222:8001}")
    private String apiDocsProdUrl;

    @Value("${app.api-docs.custom-url:}")
    private String apiDocsCustomUrl;

    @Value("${app.support-email:support@cityhelp.com}")
    private String supportEmail;

    @Value("${app.dev-email:dev@cityhelp.com}")
    private String devEmail;

    @Value("${app.github-repo:https://github.com/cityhelp/auth-service}")
    private String githubRepo;

    @Value("${app.terms-url:https://cityhelp.com/terms}")
    private String termsUrl;

    /**
     * Configures the OpenAPI specification for the Auth Service.
     *
     * This method defines the complete API documentation structure including:
     * - Service information and metadata
     * - Authentication schemes (JWT Bearer tokens)
     * - Server endpoints for different environments
     * - Global security requirements
     *
     * @return Configured OpenAPI instance with full API documentation
     */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "Bearer Authentication";

        // Build list of servers dynamically based on configuration
        List<Server> servers = new java.util.ArrayList<>();

        // Add development server
        servers.add(new Server()
                .url(apiDocsDevUrl)
                .description("Development server"));

        // Add production server (if different from dev)
        if (!apiDocsProdUrl.equals(apiDocsDevUrl)) {
            servers.add(new Server()
                    .url(apiDocsProdUrl)
                    .description("Production server"));
        }

        // Add custom server (if provided)
        if (apiDocsCustomUrl != null && !apiDocsCustomUrl.isBlank()) {
            servers.add(new Server()
                    .url(apiDocsCustomUrl)
                    .description("Custom server"));
        }

        return new OpenAPI()
                .info(apiInfo())
                .servers(servers)
                // Note: Security is applied per-endpoint using @SecurityRequirement annotation
                // Do NOT add global security requirement - it would require auth for all endpoints
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from /api/auth/login or /api/auth/register endpoints")
                        )
                );
    }

    /**
     * Defines the API metadata information displayed in Swagger UI.
     *
     * This includes service description, version, contact information,
     * license details, and terms of service.
     *
     * @return Info object with complete API metadata
     */
    private Info apiInfo() {
        return new Info()
                .title("CityHelp Auth Service API")
                .version("1.0.0")
                .description("""
                        CityHelp Authentication and Authorization Service

                        This service provides secure authentication and authorization for the CityHelp platform.

                        Key Features:
                        - JWT-based authentication (Access: 24h, Refresh: 7d)
                        - Email verification with 6-digit codes
                        - OAuth2 Google Sign-In integration
                        - Rate limiting for brute force protection
                        - Token refresh and revocation
                        - User profile management
                        - Public JWKS endpoint for token verification

                        Authentication Flow:
                        1. Register: POST /api/auth/register - Returns access + refresh tokens
                        2. Verify Email: POST /api/auth/verify-email - Activates account
                        3. Login: POST /api/auth/login - Returns new tokens
                        4. Use Token: Add header 'Authorization: Bearer {access_token}'
                        5. Refresh: POST /api/auth/refresh - Get new access token
                        6. Logout: POST /api/auth/logout - Revokes tokens

                        User States:
                        - pending_verification: Registered but email not verified
                        - active: Verified and can access the system
                        - suspended: Temporarily disabled by admin
                        - deleted: Account permanently removed

                        Rate Limiting:
                        - Login: 5 attempts per 15 minutes per IP
                        - Register: 3 attempts per hour per IP
                        - Verify Email: 5 attempts per 15 minutes per user

                        External Service Integration:
                        External services can verify JWT tokens using our public JWKS endpoint:
                        - GET /.well-known/jwks.json - Returns RSA public keys

                        Support:
                        For issues or questions, contact: """ + supportEmail + """
                        """)
                .contact(new Contact()
                        .name("CityHelp Development Team")
                        .email(devEmail)
                        .url(githubRepo))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"))
                .termsOfService(termsUrl);
    }
}
