# Multi-stage build for CityHelp Auth Service
# Stage 1: Build the application
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1001 -S cityhelp && \
    adduser -u 1001 -S cityhelp -G cityhelp

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create directory for logs
RUN mkdir -p /app/logs && chown -R cityhelp:cityhelp /app

# Switch to non-root user
USER cityhelp

# Expose port
EXPOSE 8001

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8001/actuator/health || exit 1

# Run application with active profile from environment variable
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILE:prod}", \
    "-jar", \
    "app.jar"]
