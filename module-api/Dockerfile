FROM openjdk:21-slim

ARG JAR_FILE=module-api/build/libs/*.jar

RUN apt-get update && apt-get install -y curl wget && rm -rf /var/lib/apt/lists/*

# OpenTelemetry Java Agent 다운로드
RUN wget -O /opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.21.0/opentelemetry-javaagent.jar


COPY ${JAR_FILE} app.jar

# Fat JAR 언팩 후 OpenTelemetry Logback Appender를 BOOT-INF/lib에 추가
RUN mkdir /tmp/app && \
    cd /tmp/app && \
    jar -xf /app.jar && \
    wget -O BOOT-INF/lib/opentelemetry-logback-appender-1.0-2.11.0-alpha.jar https://repo1.maven.org/maven2/io/opentelemetry/instrumentation/opentelemetry-logback-appender-1.0/2.11.0-alpha/opentelemetry-logback-appender-1.0-2.11.0-alpha.jar && \
    wget -O BOOT-INF/lib/opentelemetry-api-1.43.0.jar https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-api/1.43.0/opentelemetry-api-1.43.0.jar && \
    wget -O BOOT-INF/lib/opentelemetry-context-1.43.0.jar https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-context/1.43.0/opentelemetry-context-1.43.0.jar && \
    jar -cfm /app-new.jar META-INF/MANIFEST.MF . && \
    mv /app-new.jar /app.jar && \
    rm -rf /tmp/app

RUN ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && echo "Asia/Seoul" > /etc/timezone

ENV SPRING_PROFILES_ACTIVE=prod
ENV OTEL_SERVICE_NAME=team3-api
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://signoz-otel-collector:4317
ENV OTEL_EXPORTER_OTLP_PROTOCOL=grpc
ENV OTEL_EXPORTER_OTLP_TIMEOUT=10000
ENV OTEL_TRACES_EXPORTER=otlp
ENV OTEL_LOGS_EXPORTER=otlp
ENV OTEL_METRICS_EXPORTER=otlp
ENV OTEL_RESOURCE_ATTRIBUTES="deployment.environment=production,service.version=1.0.0"
ENV OTEL_EXPERIMENTAL_EXPORTER_OTLP_RETRY_ENABLED=true


ENTRYPOINT ["java", "-javaagent:/opentelemetry-javaagent.jar", "-Duser.timezone=Asia/Seoul", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "app.jar"]