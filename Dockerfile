# Runtime only image (Java 25)
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copy built jar from Jenkins
COPY target/w2w-api-*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
