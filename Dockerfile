# syntax=docker/dockerfile:1
FROM gradle:7-jdk11 AS build
WORKDIR /app

COPY build.gradle settings.gradle* gradle.properties* ./
COPY gradle ./gradle
COPY gradlew ./
RUN chmod +x gradlew || true
RUN gradle --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src
RUN gradle --no-daemon clean build -x test

FROM eclipse-temurin:11-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/ ./libs/

EXPOSE 8080
CMD ["sh", "-c", "exec java -jar $(ls /app/libs/*.jar | grep -v -- '-plain' | head -1)"]
