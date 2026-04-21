# Runtime only image (best practice)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy built jar from Jenkins
COPY target/w2w-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
