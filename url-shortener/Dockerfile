# Multi-stage build for URL Shortener Spring Boot Application
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Maven files
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Resolve dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src src
RUN ./mvnw clean package -DskipTests

# Runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy packaged jar and SQLite database
COPY --from=build /app/target/url-shortener-1.0.0.jar app.jar

EXPOSE 8081
ENV PORT=8081

ENTRYPOINT ["java", "-jar", "app.jar"]
