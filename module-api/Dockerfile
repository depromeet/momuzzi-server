# Stage 1: Build stage  
FROM openjdk:21-jdk-slim AS builder

WORKDIR /app

# 패키지 업데이트 및 필요한 도구 설치
RUN apt-get update && apt-get install -y curl unzip && rm -rf /var/lib/apt/lists/*

# Gradle 직접 설치
RUN curl -L https://services.gradle.org/distributions/gradle-8.14.3-bin.zip -o gradle.zip && \
    unzip gradle.zip && \
    mv gradle-8.14.3 /opt/gradle && \
    rm gradle.zip
ENV PATH="/opt/gradle/bin:$PATH"

# 프로젝트 설정 파일들 복사
COPY gradle.properties settings.gradle.kts ./
COPY build.gradle.kts ./
COPY module-api/build.gradle.kts ./module-api/
COPY module-domain/build.gradle.kts ./module-domain/
COPY module-infra/build.gradle.kts ./module-infra/
COPY module-global-utils/build.gradle.kts ./module-global-utils/

# 소스 코드 먼저 복사
COPY module-api/src ./module-api/src
COPY module-domain/src ./module-domain/src
COPY module-infra/src ./module-infra/src
COPY module-global-utils/src ./module-global-utils/src

RUN gradle :module-api:bootJar --no-daemon -x test --stacktrace

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl bash unzip git ca-certificates tzdata && \
    update-ca-certificates && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apk del tzdata

# 보안을 위한 non-root 사용자 생성
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/module-api/build/libs/*.jar app.jar

# 파일 소유권 변경 후 사용자 전환
RUN chown app:app app.jar
USER app

# 환경 변수 설정
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Duser.timezone=Asia/Seoul"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar app.jar"]