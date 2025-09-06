# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

ENV GRADLE_USER_HOME=/gradle

# Gradle Wrapper와 설정 파일들 복사 (캐시 최적화)
COPY gradlew gradle.properties settings.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew

# 프로젝트 빌드 파일들 복사
COPY build.gradle.kts ./
COPY module-api/build.gradle.kts ./module-api/
COPY module-domain/build.gradle.kts ./module-domain/
COPY module-infra/build.gradle.kts ./module-infra/
COPY module-global-utils/build.gradle.kts ./module-global-utils/

# 의존성 다운로드 (캐시 최적화)
RUN ./gradlew --no-daemon --build-cache dependencies

# 소스 코드 복사
COPY module-api/src ./module-api/src
COPY module-domain/src ./module-domain/src
COPY module-infra/src ./module-infra/src
COPY module-global-utils/src ./module-global-utils/src

# 애플리케이션 빌드 (테스트 스킵)
RUN ./gradlew :module-api:bootJar --no-daemon --build-cache -x test

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache tzdata && \
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