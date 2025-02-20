# Build stage
FROM maven:3.6.3-openjdk-17 AS build

# Set the working directory to /app
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml /app/
COPY src /app/src

# Build the application
RUN mvn clean install -X

# Run stage
FROM eclipse-temurin:17-jdk-alpine

# Set the working directory
WORKDIR /app

# Expose the /tmp directory as a volume
VOLUME /tmp

# Copy the built JAR file from the build stage
COPY --from=build /app/target/*.jar /app/app.jar

# Set the entrypoint for the container
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
