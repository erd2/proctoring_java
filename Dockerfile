# Multi-stage Docker build for Proctoring System
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Install Gradle
RUN apk add --no-cache wget unzip
RUN wget https://services.gradle.org/distributions/gradle-8.10-bin.zip -O /tmp/gradle.zip && \
    unzip -d /opt/gradle /tmp/gradle.zip && \
    ln -s /opt/gradle/gradle-8.10/bin/gradle /usr/local/bin/gradle && \
    rm /tmp/gradle.zip

# Copy source
COPY . .

# Make gradlew executable (if exists)
RUN chmod +x ./gradlew || true

# Build application
RUN gradle --no-daemon clean build -x test

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
