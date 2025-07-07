# Multi-stage Dockerfile for Telemetry Service

# Stage 1: Build
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre

# Add non-root user
RUN groupadd -g 1001 telemetry && \
    useradd -u 1001 -g telemetry -m telemetry

# Set working directory
WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/target/telemetry-service-*.jar app.jar

# Change ownership
RUN chown -R telemetry:telemetry /app

# Switch to non-root user
USER telemetry

# Expose port
EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8084/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"] 