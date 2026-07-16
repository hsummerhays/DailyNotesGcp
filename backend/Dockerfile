# Build Stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy gradle files for caching
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle gradle

# Grant execution rights on the gradlew script
RUN chmod +x gradlew

# Download dependencies (using a dummy task to cache them if possible, or build without source)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code and build
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# Run Stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Expose server port
EXPOSE 8080

# Configure healthcheck using Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
