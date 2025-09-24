# Multi-stage build
FROM maven:3.9.9-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy parent pom and module structure (now we're building from parent directory)
COPY pom.xml ./
COPY logging-service/pom.xml ./logging-service/
COPY logging-service/src ./logging-service/src
COPY resource-api/pom.xml ./resource-api/

# Build the specific service - Spring Boot plugin will automatically repackage during package phase
RUN mvn clean package -pl logging-service -am -DskipTests

# Production stage
FROM eclipse-temurin:17-jre-alpine


# Set working directory
WORKDIR /app

# Copy the built jar file from builder stage
COPY --from=builder /app/logging-service/target/logging-service-*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
