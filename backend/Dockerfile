FROM maven:3.9.11-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser && mkdir -p /app/data && chown -R appuser:appuser /app
COPY --from=builder /workspace/release-output/*.jar app.jar
USER appuser
EXPOSE 18083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
