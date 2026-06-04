# Use an official OpenJDK image
FROM eclipse-temurin:22-jdk-jammy

# Build-time arguments
ARG PROFILE

# Set work directory
WORKDIR /app

# Copy the built jar file into the container
COPY target/dsl-dreamlink-identity-service.jar dsl-dreamlink-identity-service.jar

# Runtime environment variables
ENV SPRING_PROFILES_ACTIVE=${PROFILE}

# Expose the port your Spring Boot app runs on
EXPOSE 8082

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "dsl-dreamlink-identity-service.jar"]
