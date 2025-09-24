# Multi-stage build
FROM maven:3.9.9-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy parent pom and module structure (now we're building from parent directory)
COPY pom.xml ./
COPY resource-api/pom.xml ./resource-api/
COPY resource-api/src ./resource-api/src
COPY logging-service/pom.xml ./logging-service/

# Build the specific service - Spring Boot plugin will automatically repackage during package phase
RUN mvn clean package -pl resource-api -am -DskipTests

# Production stage
FROM eclipse-temurin:17-jre-alpine

# Set working directory
WORKDIR /app

# Copy the built jar file from builder stage
COPY --from=builder /app/resource-api/target/resource-api-*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]