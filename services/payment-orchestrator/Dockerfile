# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY build-conventions/ build-conventions/
COPY services/payment-orchestrator/ services/payment-orchestrator/
RUN ./mvnw -B -pl services/payment-orchestrator -am package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update     && apt-get install -y --no-install-recommends curl     && rm -rf /var/lib/apt/lists/*     && groupadd --system --gid 1000 payhub     && useradd --system --uid 1000 --gid payhub payhub
WORKDIR /app
COPY --from=build /workspace/services/payment-orchestrator/target/*.jar /app/app.jar
USER 1000
EXPOSE 8084
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3   CMD curl -fsS http://127.0.0.1:8084/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

