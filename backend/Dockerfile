# ============================================
# Stage 1: Build the application with Maven
# ============================================
FROM eclipse-temurin:17-jdk-alpine AS builder

# Install Maven
RUN apk add --no-cache maven

WORKDIR /workspace

# Copy parent POM first (for dependency resolution)
COPY pom.xml /workspace/pom.xml

# Copy backend module
COPY backend/pom.xml /workspace/backend/pom.xml

# Download dependencies (cached layer)
WORKDIR /workspace/backend
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B || true

# Copy source code
COPY backend/src /workspace/backend/src

# Build the application (skip tests for faster builds)
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B

# ============================================
# Stage 2: Runtime image (minimal, secure)
# ============================================
FROM eclipse-temurin:17-jre-alpine

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /workspace/backend/target/backend-*.jar app.jar

# Change ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8082

# Health check endpoint
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# JVM tuning for containerized environments
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
