FROM gradle:8-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle --no-daemon clean build -x test

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/app.jar
EXPOSE 8000
CMD ["sh", "-c", "exec java -jar $(ls /app/app.jar | head -1)"]
