FROM openjdk:21-slim

ARG JAR_FILE=build/libs/*.jar

RUN apt-get update && apt-get install -y curl wget && rm -rf /var/lib/apt/lists/*

# OpenTelemetry Java Agent 다운로드
RUN wget -O /opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.21.0/opentelemetry-javaagent.jar

COPY ${JAR_FILE} app.jar

RUN ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && echo "Asia/Seoul" > /etc/timezone

ENV SPRING_PROFILES_ACTIVE=prod
ENV OTEL_SERVICE_NAME=team3-api
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://signoz-otel-collector:4317
ENV OTEL_EXPORTER_OTLP_PROTOCOL=grpc
ENV OTEL_TRACES_EXPORTER=otlp
ENV OTEL_LOGS_EXPORTER=otlp
ENV OTEL_METRICS_EXPORTER=otlp
ENV OTEL_RESOURCE_ATTRIBUTES="deployment.environment=production,service.version=1.0.0"


ENTRYPOINT ["java", "-javaagent:/opentelemetry-javaagent.jar", "-Duser.timezone=Asia/Seoul", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "app.jar"]