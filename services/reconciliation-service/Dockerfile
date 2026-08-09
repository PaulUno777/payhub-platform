# syntax=docker/dockerfile:1
# Build from repository root:
#   docker build -f services/reconciliation-service/Dockerfile -t pauluno/payhub-reconciliation-service:local .

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY . .
RUN ./mvnw -B -pl services/reconciliation-service -am package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 1000 payhub \
    && useradd --system --uid 1000 --gid payhub payhub
WORKDIR /app
COPY --from=build /workspace/services/reconciliation-service/target/*.jar /app/app.jar
USER 1000
EXPOSE 8080 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
