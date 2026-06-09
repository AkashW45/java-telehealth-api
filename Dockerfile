# Stage 1: build using the project's own pinned Gradle (via wrapper)
FROM eclipse-temurin:11-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew clean assemble --no-daemon

# Stage 2: slim runtime
FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
