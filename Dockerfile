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

# Create directory for logs with proper permissions
RUN mkdir -p /app/logs && \
    chown -R cityhelp:cityhelp /app && \
    chmod 755 /app && \
    chmod 775 /app/logs

# Switch to non-root user
USER cityhelp

# Expose port (use $PORT for Render, fallback to 8001 for local)
EXPOSE ${PORT:-8001}

# Health check (use $PORT environment variable)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8001}/actuator/health || exit 1

# Run application with active profile and dynamic port
# Optimized for small VPS (2 CPU, 3.7GB RAM)
# -Xmx512m: Max heap 512MB (reduce if OOM)
# -Xms256m: Initial heap 256MB
# -XX:+UseG1GC: G1 Garbage Collector (better for small heaps)
# -XX:MaxGCPauseMillis=200: GC pause target
# -XX:+ParallelRefProcEnabled: Parallel reference processing
ENTRYPOINT ["sh", "-c", "java \
    -Xmx512m \
    -Xms256m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+ParallelRefProcEnabled \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=${SPRING_PROFILE:prod} \
    -Dserver.port=${PORT:-8001} \
    -jar app.jar"]
