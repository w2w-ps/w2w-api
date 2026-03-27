# Multi-stage build for Spring Boot
FROM maven:3.9.14-eclipse-temurin-25-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/w2w-api-0.0.1-SNAPSHOT.jar app.jar

# Standard Spring Boot port
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
