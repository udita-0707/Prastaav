# Builds the API with Maven and runs the packaged jar on a JRE as a non-root user.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B -DskipTests package

# Not the Alpine variant: it has no arm64 build for Java 17, so it would not run on Apple Silicon.
FROM eclipse-temurin:17-jre
RUN groupadd --system prastav && useradd --system --gid prastav prastav
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER prastav
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
