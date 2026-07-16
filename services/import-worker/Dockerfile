# Build Stage
# Built from the repo root (see docker-compose.yml) so this multi-project Gradle
# build can see the shared services/common module alongside its own sources.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Root Gradle wrapper and project layout
COPY gradlew gradlew.bat settings.gradle ./
COPY gradle gradle

# Module build files needed to configure the multi-project build
COPY services/common/build.gradle services/common/build.gradle
COPY services/notes-api/build.gradle services/notes-api/build.gradle
COPY services/import-worker/build.gradle services/import-worker/build.gradle

RUN chmod +x gradlew

# Download dependencies for this service and its shared module
RUN ./gradlew :services:import-worker:dependencies --no-daemon

# Copy source code and build just this service's jar
COPY services/common/src services/common/src
COPY services/import-worker/src services/import-worker/src
RUN ./gradlew :services:import-worker:bootJar -x test --no-daemon

# Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as a dedicated non-root user rather than the image default of root
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build --chown=spring:spring /workspace/services/import-worker/build/libs/*.jar app.jar
USER spring

# Expose server port for actuator
EXPOSE 8081

# Configure healthcheck using Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
