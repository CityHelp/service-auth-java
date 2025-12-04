# CityHelp Auth Service

**Enterprise-Grade Authentication & Authorization Server**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Endpoints](#api-endpoints)
- [Database](#database)
- [Security](#security)
- [Testing](#testing)
- [Docker Deployment](#docker-deployment)
- [Production Deployment](#production-deployment)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

**CityHelp Auth Service** is a centralized authentication and authorization server built with Spring Boot that serves as the single source of truth for user authentication across the CityHelp ecosystem. The service provides JWT-based authentication with support for both traditional email/password login and OAuth2 social authentication (Google).

### Key Responsibilities

- **JWT Token Issuance**: Issues signed JWT access tokens (24-hour expiration) and refresh tokens (7-day expiration) using RSA key pairs
- **Public Key Exposure**: Exposes a JWKS (JSON Web Key Set) endpoint at `/.well-known/jwks.json` for external services to validate tokens without database access
- **User Management**: Handles user registration, email verification, account management, and authentication
- **OAuth2 Integration**: Provides social login capabilities through Google OAuth2 with seamless user account creation
- **Microservices Support**: Enables external services (C#, Python, Node.js, etc.) to verify user identity independently

### Project Context

This service is part of a multi-platform architecture where:
- **Backend Services** (C#, Python, etc.) verify JWTs using the public JWKS endpoint
- **Mobile Applications** (Android/Kotlin) consume authentication APIs and manage JWT tokens
- **Centralized Authentication** ensures consistent security policies across all platforms

---

## Features

### Authentication

- **Traditional Login**: Email/username and password authentication with BCrypt hashing
- **User Registration**: Account creation with mandatory email verification (6-digit code, 15-minute expiration)
- **Email Verification**: SMTP-based verification emails with HTML templates
- **OAuth2 Social Login**: Google authentication with automatic account creation (pre-verified)
- **Password Management**: Secure password change functionality
- **Account Deletion**: Physical deletion of user accounts from database

### Token Management

- **JWT Access Tokens**: Short-lived tokens (24 hours) for API authorization
- **Refresh Tokens**: Long-lived tokens (7 days) for obtaining new access tokens
- **Token Revocation**: Logout functionality that revokes all user refresh tokens
- **JWKS Endpoint**: Public key distribution for external services (`/.well-known/jwks.json`)

### Security

- **Password Policy**: Minimum 8 characters, requires uppercase, number, and special character
- **Email Verification**: Mandatory for local registrations (OAuth2 users pre-verified)
- **Rate Limiting**: Redis-based rate limiting to prevent brute force attacks (planned)
- **CSRF Protection**: Spring Security default protections enabled
- **Secure Password Storage**: BCrypt hashing algorithm

### User States

- **pending_verification**: New local users awaiting email verification
- **active**: Verified users who can access the system
- **suspended**: Administratively suspended accounts
- **deleted**: Permanently removed accounts

### Provider Support

- **LOCAL**: Traditional email/password authentication
- **GOOGLE**: OAuth2 Google authentication with seamless account linking

---

## Architecture

### Clean Architecture Layers

The service follows **Clean Architecture** principles with clear separation of concerns:

```
src/main/java/com/crudzaso/cityhelp/auth/
├── domain/                     # Business logic and entities (pure Java)
│   ├── model/                  # Domain entities (User, RefreshToken, etc.)
│   ├── enums/                  # Business enums (UserStatus, UserRole, OAuthProvider)
│   └── repository/             # Repository interfaces (ports)
│
├── application/                # Use cases and business workflows
│   ├── service/                # Business services
│   └── usecase/                # Use case implementations
│
└── infrastructure/             # External adapters and frameworks
    ├── controller/             # REST API controllers
    ├── repository/             # JPA repository implementations
    ├── security/               # Spring Security configuration
    ├── config/                 # Spring configuration classes
    └── email/                  # Email service implementation
```

### Architecture Benefits

- **Domain Independence**: Core business logic has no framework dependencies
- **Testability**: Easy to unit test domain logic in isolation
- **Maintainability**: Clear boundaries between layers reduce coupling
- **Flexibility**: Easy to swap infrastructure components (database, email provider, etc.)

---

## Technology Stack

### Core Framework

- **Spring Boot**: 3.5.8
- **Java**: 21 (LTS)
- **Build Tool**: Apache Maven 3.9+

### Data & Persistence

- **Database**: PostgreSQL 15
- **ORM**: Spring Data JPA + Hibernate
- **Migrations**: Flyway (automatic on startup)
- **Connection Pooling**: HikariCP

### Security

- **Authentication**: Spring Security 6
- **JWT Library**: JJWT (io.jsonwebtoken)
- **OAuth2**: Spring OAuth2 Client
- **Password Hashing**: BCrypt

### Messaging & Caching

- **Email**: Spring Mail (SMTP with Gmail)
- **Caching**: Redis (for rate limiting and session management)

### Monitoring & Operations

- **Actuator**: Spring Boot Actuator (health checks, metrics)
- **Metrics**: Prometheus-compatible endpoints
- **Logging**: SLF4J + Logback

### Development Tools

- **Hot Reload**: Spring Boot DevTools
- **Configuration Processor**: Spring Boot Configuration Processor
- **Lombok**: Code generation for POJOs

### Testing

- **Unit Testing**: JUnit 5, Mockito, AssertJ
- **Integration Testing**: Spring Boot Test, Testcontainers (in development)
- **Code Coverage**: JaCoCo (planned)

---

## Prerequisites

Ensure the following software is installed on your system:

- **Java Development Kit (JDK)**: 21 or higher
  ```bash
  java -version
  # Output: openjdk version "21.0.x"
  ```

- **Apache Maven**: 3.9 or higher
  ```bash
  mvn -version
  # Output: Apache Maven 3.9.x
  ```

- **PostgreSQL**: 15 or higher
  ```bash
  psql --version
  # Output: psql (PostgreSQL) 15.x
  ```

- **Redis**: 7.0 or higher (optional, for rate limiting)
  ```bash
  redis-cli --version
  # Output: redis-cli 7.0.x
  ```

- **Docker**: 24.0+ and Docker Compose 2.0+ (optional, for containerized deployment)
  ```bash
  docker --version
  docker-compose --version
  ```

---

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/CityHelp/service-auth-java.git
cd service-auth-java
```

### 2. Database Setup

Create PostgreSQL database and user:

```sql
-- Connect to PostgreSQL as superuser
psql -U postgres

-- Create database
CREATE DATABASE cityhelp_auth;

-- Create user with password
CREATE USER auth_service_user WITH PASSWORD '[YOUR_SECURE_DB_PASSWORD]';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE cityhelp_auth TO auth_service_user;
GRANT ALL ON SCHEMA public TO auth_service_user;

-- Exit
\q
```

### 3. Environment Configuration

Copy the example environment file:

```bash
cp .env.example .env
```

Edit `.env` and configure your values (see [Configuration](#configuration) section).

### 4. Build the Project

```bash
mvn clean install
```

This will:
- Download dependencies
- Compile source code
- Run unit tests
- Package the application as a JAR

---

## Configuration

### Environment Variables

Create a `.env` file in the project root with the following variables:

#### Database Configuration

```bash
# PostgreSQL Database
DB_URL=jdbc:postgresql://localhost:5432/cityhelp_auth
DB_USERNAME=auth_service_user
DB_PASSWORD=[YOUR_DB_PASSWORD]
```

#### JWT Configuration

```bash
# JWT Secret Key (use a strong random string, min 256-bit)
JWT_SECRET=[YOUR_JWT_SECRET_MIN_32_CHARACTERS]

# JWT Access Token Expiration (milliseconds - default: 24 hours)
JWT_EXPIRATION=86400000

# JWT Refresh Token Expiration (milliseconds - default: 7 days)
JWT_REFRESH_EXPIRATION=604800000

# RSA Keys for JWKS (auto-generated if not provided)
JWT_PRIVATE_KEY=classpath:keys/private_key.pem
JWT_PUBLIC_KEY=classpath:keys/public_key.pem

# JWKS Key ID
JWT_KEY_ID=cityhelp-key-1
```

#### OAuth2 Configuration

```bash
# Google OAuth2 Credentials (get from Google Cloud Console)
GOOGLE_CLIENT_ID=[YOUR_GOOGLE_CLIENT_ID]
GOOGLE_CLIENT_SECRET=[YOUR_GOOGLE_CLIENT_SECRET]

# OAuth2 Redirect URI
OAUTH2_REDIRECT_URI=http://localhost:8001/oauth2/redirect
```

#### Email Configuration (SMTP)

```bash
# Gmail SMTP Configuration
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=noreply@cityhelp.com
SMTP_PASSWORD=[YOUR_SMTP_APP_PASSWORD]

# Email sender information
EMAIL_FROM=noreply@cityhelp.com
EMAIL_FROM_NAME=CityHelp Team
```

#### Redis Configuration

```bash
# Redis for rate limiting and caching
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
```

#### Application Configuration

```bash
# Server Port
SERVER_PORT=8001

# Active Profile (dev, staging, prod)
SPRING_PROFILES_ACTIVE=dev
```

### Google OAuth2 Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable "Google+ API"
4. Navigate to "APIs & Services" > "Credentials"
5. Create "OAuth 2.0 Client ID" (Web application)
6. Add authorized redirect URI:
   - Development: `http://localhost:8001/login/oauth2/code/google`
   - Production: `https://yourdomain.com/login/oauth2/code/google`
7. Copy Client ID and Client Secret to `.env`

---

## Running the Application

### Local Development

#### Option 1: Using Maven

```bash
# Load environment variables and run
export $(cat .env | xargs) && mvn spring-boot:run
```

#### Option 2: Using Java JAR

```bash
# Build JAR
mvn clean package -DskipTests

# Load environment variables and run JAR
export $(cat .env | xargs) && java -jar target/cityhelp-0.0.1-SNAPSHOT.jar
```

#### Option 3: Using IDE

1. Import project into IntelliJ IDEA or Eclipse
2. Configure environment variables in Run Configuration
3. Run `CityHelpAuthApplication.java`

### Verify Application Started

```bash
# Check application health
curl http://localhost:8001/actuator/health

# Expected output:
# {"status":"UP"}
```

### Access Points

- **Application**: http://localhost:8001
- **Actuator Health**: http://localhost:8001/actuator/health
- **Actuator Metrics**: http://localhost:8001/actuator/metrics
- **JWKS Endpoint**: http://localhost:8001/.well-known/jwks.json
- **API Documentation**: http://localhost:8001/swagger-ui.html (planned)

---

## API Endpoints

### Authentication Endpoints

#### Register User

```http
POST /api/auth/register
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "[YOUR_TEST_PASSWORD]"
}
```

**Response** (201 Created):
```json
{
  "message": "User registered successfully. Please verify your email.",
  "userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### Verify Email

```http
POST /api/auth/verify-email
Content-Type: application/json

{
  "email": "john.doe@example.com",
  "code": "123456"
}
```

**Response** (200 OK):
```json
{
  "message": "Email verified successfully. Your account is now active."
}
```

#### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "john.doe@example.com",
  "password": "[YOUR_TEST_PASSWORD]"
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

#### OAuth2 Google Login

```http
GET /oauth2/authorization/google
```

Redirects to Google consent screen. After authorization, redirects back with tokens.

#### Refresh Token

```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "660e8400-e29b-41d4-a716-446655440001",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

#### Logout

```http
POST /api/auth/logout
Authorization: Bearer {accessToken}
```

**Response** (200 OK):
```json
{
  "message": "Logged out successfully"
}
```

### User Management Endpoints

#### Get Current User

```http
GET /api/auth/me
Authorization: Bearer {accessToken}
```

**Response** (200 OK):
```json
{
  "id": 1,
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "role": "USER",
  "status": "ACTIVE",
  "oauthProvider": "LOCAL",
  "isVerified": true,
  "createdAt": "2025-01-15T10:30:00",
  "lastLoginAt": "2025-01-16T14:22:00"
}
```

#### Change Password

```http
PUT /api/auth/change-password
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "currentPassword": "OldP@ss123",
  "newPassword": "NewSecureP@ss456"
}
```

**Response** (200 OK):
```json
{
  "message": "Password changed successfully"
}
```

#### Delete Account

```http
DELETE /api/auth/delete-account
Authorization: Bearer {accessToken}
```

**Response** (200 OK):
```json
{
  "message": "Account deleted permanently"
}
```

### JWKS Endpoint (Public Key)

#### Get JSON Web Key Set

```http
GET /.well-known/jwks.json
```

**Response** (200 OK):
```json
{
  "keys": [
    {
      "kty": "RSA",
      "e": "AQAB",
      "use": "sig",
      "kid": "cityhelp-key-1",
      "alg": "RS256",
      "n": "xGOr-H7A-PWnP5..."
    }
  ]
}
```

This endpoint is used by external services to verify JWT signatures without accessing the database.

---

## Database

### Schema

The database schema is managed by **Flyway** migrations located in `src/main/resources/db/migration/`.

#### Tables

**users**
- `id` (BIGSERIAL, PK)
- `uuid` (UUID, UNIQUE)
- `first_name` (VARCHAR 100)
- `last_name` (VARCHAR 100)
- `email` (VARCHAR 255, UNIQUE)
- `password` (VARCHAR 255, nullable for OAuth2)
- `oauth_provider` (ENUM: LOCAL, GOOGLE)
- `is_verified` (BOOLEAN)
- `status` (ENUM: PENDING_VERIFICATION, ACTIVE, DELETED, SUSPENDED)
- `role` (ENUM: USER, ADMIN)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)
- `last_login_at` (TIMESTAMP)

**refresh_tokens**
- `id` (BIGSERIAL, PK)
- `token` (VARCHAR 500, UNIQUE)
- `user_id` (BIGINT, FK → users.id)
- `expires_at` (TIMESTAMP)
- `created_at` (TIMESTAMP)
- `is_revoked` (BOOLEAN)

**email_verification_codes**
- `id` (BIGSERIAL, PK)
- `user_id` (BIGINT, FK → users.id)
- `code` (VARCHAR 6)
- `expires_at` (TIMESTAMP)
- `created_at` (TIMESTAMP)
- `is_used` (BOOLEAN)
- `attempts` (INT)

### Migrations

Flyway automatically applies migrations on application startup:

```bash
# View migration status
mvn flyway:info

# Manually apply migrations (not needed if auto-migration is enabled)
mvn flyway:migrate

# Rollback to specific version (use with caution)
mvn flyway:undo
```

**Migration Files:**
- `V1__create_users_table.sql` - Creates users table with enums
- `V2__create_refresh_tokens_table.sql` - Creates refresh tokens table
- `V3__create_email_verification_codes_table.sql` - Creates email verification table

---

## Security

### JWT Token Flow

1. **User Login**: User provides credentials (email/password or OAuth2)
2. **Authentication**: Spring Security validates credentials
3. **Token Generation**: Service generates:
   - **Access Token**: RS256-signed JWT (24-hour expiration)
   - **Refresh Token**: UUID stored in database (7-day expiration)
4. **Token Response**: Client receives both tokens
5. **API Authorization**: Client includes `Authorization: Bearer {accessToken}` header
6. **Token Verification**: Spring Security validates JWT signature using public key
7. **Token Refresh**: Client uses refresh token to obtain new access token when expired

### JWKS Architecture

External services verify JWTs without database access:

```
┌─────────────────┐
│ External Service│
│  (C#/Python)    │
└────────┬────────┘
         │ 1. Fetch JWKS
         ▼
┌─────────────────┐
│ /.well-known/   │
│   jwks.json     │◄─── Auth Service
└────────┬────────┘
         │ 2. Get Public Key
         ▼
┌─────────────────┐
│ Verify JWT      │
│ Signature       │
└─────────────────┘
```

### OAuth2 Flow

1. User clicks "Login with Google"
2. Redirect to `/oauth2/authorization/google`
3. Spring redirects to Google consent screen
4. User authorizes application
5. Google redirects to `/login/oauth2/code/google` with authorization code
6. `CustomOAuth2UserService` exchanges code for user info
7. Service creates or updates user (status=ACTIVE, oauth_provider=GOOGLE)
8. `OAuth2AuthenticationSuccessHandler` generates JWT tokens
9. Redirect to `/oauth2/redirect` with tokens in URL parameters

### Security Best Practices

- **BCrypt Password Hashing**: Salt rounds = 10
- **HTTPS Only in Production**: Enforce SSL/TLS
- **CSRF Protection**: Enabled by default in Spring Security
- **Rate Limiting**: Redis-based (planned implementation)
- **Token Expiration**: Short-lived access tokens, longer refresh tokens
- **Secure Headers**: X-Content-Type-Options, X-Frame-Options, etc.

---

## Testing

### Running Tests

```bash
# Run all unit tests
mvn test

# Run integration tests (in development)
mvn integration-test

# Generate code coverage report
mvn jacoco:report
```

### Test Structure

```
src/test/java/com/crudzaso/cityhelp/auth/
├── domain/          # Domain entity tests
├── application/     # Use case tests
└── infrastructure/  # Controller and integration tests
```

### Testing Strategy

- **Unit Tests**: JUnit 5 + Mockito for isolated component testing
- **Integration Tests**: Testcontainers for database-dependent tests (in development)
- **API Tests**: MockMVC for controller endpoint testing
- **Code Coverage Target**: 80%+

**Note**: Comprehensive test suite is currently in development by team member on `feature/testing` branch.

---

## Docker Deployment

### Build Docker Image

```bash
# Build application JAR
mvn clean package -DskipTests

# Build Docker image
docker build -t cityhelp-auth:latest .
```

### Docker Compose

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: cityhelp_auth
      POSTGRES_USER: auth_service_user
      POSTGRES_PASSWORD: [YOUR_POSTGRES_PASSWORD]
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  auth-service:
    image: cityhelp-auth:latest
    ports:
      - "8001:8001"
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/cityhelp_auth
      DB_USERNAME: auth_service_user
      DB_PASSWORD: [YOUR_DB_PASSWORD]
      REDIS_HOST: redis
      REDIS_PORT: 6379
    depends_on:
      - postgres
      - redis

volumes:
  postgres_data:
```

### Run with Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f auth-service

# Stop all services
docker-compose down
```

---

## Production Deployment

### Pre-Deployment Checklist

- [ ] Update `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` with production credentials
- [ ] Configure production OAuth2 redirect URI (HTTPS): `https://yourdomain.com/login/oauth2/code/google`
- [ ] Generate strong `JWT_SECRET` (minimum 256-bit random string)
- [ ] Set `SPRING_PROFILES_ACTIVE=prod`
- [ ] Configure production database credentials
- [ ] Set up SSL/TLS certificates
- [ ] Enable HTTPS redirect in Spring Security
- [ ] Configure SMTP with production email service
- [ ] Set up monitoring and alerting (Prometheus + Grafana)
- [ ] Configure backup strategy for PostgreSQL
- [ ] Set up log aggregation (ELK stack or similar)

### Environment-Specific Configuration

Create `application-prod.yml` for production:

```yaml
server:
  port: 8001
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12

spring:
  jpa:
    show-sql: false  # Disable SQL logging in production

logging:
  level:
    root: WARN
    com.crudzaso.cityhelp: INFO
```

### Health Checks

Configure Kubernetes liveness and readiness probes:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8001
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8001
  initialDelaySeconds: 20
  periodSeconds: 5
```

---

## Troubleshooting

### Common Issues

#### Database Connection Failed

**Error**: `org.postgresql.util.PSQLException: Connection refused`

**Solution**:
- Verify PostgreSQL is running: `sudo systemctl status postgresql`
- Check database credentials in `.env`
- Ensure database exists: `psql -U postgres -c "\l"`
- Verify network connectivity: `telnet localhost 5432`

#### Flyway Migration Failed

**Error**: `FlywayException: Validate failed: Migration checksum mismatch`

**Solution**:
```bash
# Option 1: Repair Flyway schema history
mvn flyway:repair

# Option 2: Drop and recreate database (development only!)
dropdb -U postgres cityhelp_auth
createdb -U postgres cityhelp_auth
```

#### JWT Token Invalid

**Error**: `401 Unauthorized - Invalid JWT signature`

**Solution**:
- Verify `JWT_SECRET` matches between token generation and validation
- Check token expiration: decode JWT at https://jwt.io
- Ensure RSA keys match (public key for verification)

#### OAuth2 Redirect URI Mismatch

**Error**: `400 Bad Request - redirect_uri_mismatch`

**Solution**:
- Verify redirect URI in Google Cloud Console matches:
  - Development: `http://localhost:8001/login/oauth2/code/google`
  - Production: `https://yourdomain.com/login/oauth2/code/google`
- Wait 5 minutes after adding URI for Google to propagate changes

#### Email Not Sending

**Error**: `MailSendException: Failed to send email`

**Solution**:
- Verify SMTP credentials in `.env`
- For Gmail: Generate App Password (not regular password)
- Check firewall rules allow SMTP port 587
- Test SMTP connection: `telnet smtp.gmail.com 587`

### Debug Mode

Enable debug logging:

```bash
# Add to .env or application.yml
LOGGING_LEVEL_COM_CRUDZASO_CITYHELP=DEBUG
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=DEBUG
```

### Support

For additional support:
- Check project documentation in `/spec` directory
- Review GitHub Issues: [https://github.com/CityHelp/service-auth-java/issues](https://github.com/CityHelp/service-auth-java/issues)
- Contact development team

---

## Contributing

We welcome contributions from the community. Please follow these guidelines:

### Development Workflow

1. **Fork the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/service-auth-java.git
   ```

2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make your changes**
   - Follow Clean Architecture principles
   - Write unit tests for new functionality
   - Update documentation as needed

4. **Commit your changes**
   ```bash
   git commit -m "feat: add your feature description"
   ```

5. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```

6. **Create a Pull Request**
   - Use descriptive PR title (feat/fix/docs/refactor)
   - Include summary of changes
   - Reference related issues
   - Ensure all tests pass

### Code Style

- **Java**: Follow Google Java Style Guide
- **Naming Conventions**:
  - Classes: PascalCase
  - Methods/Variables: camelCase
  - Constants: UPPER_SNAKE_CASE
  - Database: snake_case
- **Clean Architecture**: Respect layer boundaries
- **Testing**: Maintain 80%+ code coverage

### Commit Message Format

```
<type>: <subject>

<body (optional)>
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

Example:
```
feat: add password reset functionality

- Create PasswordResetToken entity
- Implement reset request endpoint
- Send reset email with secure token
```

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Project Information

- **Version**: 0.0.1-SNAPSHOT
- **Spring Boot**: 3.5.8
- **Java**: 21
- **Build Tool**: Maven 3.9+
- **Database**: PostgreSQL 15
- **Repository**: [https://github.com/CityHelp/service-auth-java](https://github.com/CityHelp/service-auth-java)

---

## Acknowledgments

Built with:
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)
- [PostgreSQL](https://www.postgresql.org/)
- [JJWT](https://github.com/jwtk/jjwt)
- [Flyway](https://flywaydb.org/)

---

**CityHelp Auth Service** - Powering secure authentication across the CityHelp ecosystem.
