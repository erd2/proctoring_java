# Multi-stage Docker build for Proctoring System
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy source and use the repository Gradle wrapper for reproducible builds
COPY . .

# Make gradlew executable
RUN chmod +x ./gradlew

# Build application
RUN ./gradlew --no-daemon clean build -x test

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy built jar
COPY --from=builder /app/proctoring-interfaces/build/libs/*.jar app.jar

# Create non-root user
RUN addgroup -g 1001 -S spring && \
    adduser -u 1001 -S spring -G spring

USER spring:spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
