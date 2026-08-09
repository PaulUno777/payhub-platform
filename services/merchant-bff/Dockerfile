# syntax=docker/dockerfile:1
# Build from repository root:
#   docker build -f services/merchant-bff/Dockerfile -t pauluno/payhub-merchant-bff:local .

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -pl services/merchant-bff -am package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 1000 payhub \
    && useradd --system --uid 1000 --gid payhub payhub
WORKDIR /app
COPY --from=build /workspace/services/merchant-bff/target/*.jar /app/app.jar
USER 1000
EXPOSE 8080 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
