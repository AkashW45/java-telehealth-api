# syntax=docker/dockerfile:1
FROM gradle:7-jdk11 AS build
WORKDIR /app

COPY build.gradle settings.gradle* gradle.properties* ./
COPY gradle ./gradle
COPY gradlew ./
RUN chmod +x gradlew || true
RUN gradle --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src

# `assemble` = compile + package only. Skips the `check` phase which includes
# compileTestJava. This repo has pre-existing broken test code (Spring annotation
# API mismatches) that fails to compile — irrelevant to producing the runtime jar.
RUN gradle --no-daemon clean assemble

FROM eclipse-temurin:11-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/ ./libs/

EXPOSE 8080
CMD ["sh", "-c", "exec java -jar $(ls /app/libs/*.jar | grep -v -- '-plain' | head -1)"]
