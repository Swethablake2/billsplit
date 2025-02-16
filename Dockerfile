# Use OpenJDK as base image
FROM openjdk:17-jdk-slim

# Install PostgreSQL client (`psql`)
RUN apt-get update && apt-get install -y postgresql-client

# Set working directory
WORKDIR /app

# Copy the built JAR file
COPY target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
