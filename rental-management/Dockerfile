# =============================================================================
# Multi-stage Dockerfile — Rental Management Spring Boot application
# =============================================================================
# Stage 1: build
#   Uses the official Maven + JDK 21 image to compile and package the fat JAR.
#   No credentials or secrets are needed at build time.
#
# Stage 2: runtime
#   Uses a slim JRE 21 image.  All sensitive configuration is injected at
#   container startup via environment variables (DB_URL, DB_USERNAME,
#   DB_PASSWORD, JWT_SECRET, JWT_EXPIRATION).
# =============================================================================

# ---------------------------------------------------------------------------
# Stage 1 — Build
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

# Copy the Maven wrapper and POM first so dependency resolution is cached
# independently of source changes.
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./

# Pre-fetch all dependencies (layer cached unless pom.xml changes).
# -B = batch mode (no progress output), -q = quiet
RUN ./mvnw dependency:go-offline -B -q

# Copy application source and build the fat JAR, skipping tests.
# Tests run separately against a live PostgreSQL instance; they are not
# meaningful inside this network-isolated build stage.
COPY src/ src/
RUN ./mvnw package -B -q -DskipTests

# ---------------------------------------------------------------------------
# Stage 2 — Runtime
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create a non-root user for security best practice.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

# Copy only the fat JAR produced by the build stage.
COPY --from=build /workspace/target/*.jar app.jar

# Document the port the application listens on.
EXPOSE 8080

# ---------------------------------------------------------------------------
# All sensitive/runtime configuration is provided via environment variables:
#
#   DB_URL          JDBC URL for PostgreSQL
#   DB_USERNAME     Database username
#   DB_PASSWORD     Database password          (required, no default)
#   JWT_SECRET      HMAC-SHA256 signing key    (required, no default, ≥32 chars)
#   JWT_EXPIRATION  Token TTL in ms            (optional, default 3600000)
#
# Flyway migrations run automatically on application startup.
# ---------------------------------------------------------------------------

# Container health check — polls the Spring Boot Actuator health endpoint.
# start_period gives the JVM + Flyway time to start before failures count.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
