FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

RUN apt-get update && apt-get install -y unzip curl && rm -rf /var/lib/apt/lists/*

COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY gradle.properties settings.gradle.kts build.gradle.kts ./

COPY module-api/build.gradle.kts ./module-api/
COPY module-domain/build.gradle.kts ./module-domain/
COPY module-infra/build.gradle.kts ./module-infra/
COPY module-global-utils/build.gradle.kts ./module-global-utils/

RUN chmod +x ./gradlew

RUN ./gradlew dependencies --no-daemon \
    -Dorg.gradle.internal.http.connectionTimeout=60000 \
    -Dorg.gradle.internal.http.socketTimeout=60000

COPY module-api/src ./module-api/src
COPY module-domain/src ./module-domain/src
COPY module-infra/src ./module-infra/src
COPY module-global-utils/src ./module-global-utils/src

# 6. 빌드 실행 (의존성은 이미 캐시됨)
RUN ./gradlew :module-api:bootJar -x test --no-daemon \
    --parallel \
    -Dorg.gradle.jvmargs="-Xmx2g -XX:MaxMetaspaceSize=512m"

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN addgroup --system app && adduser --system --group app

# JAR 파일 복사
COPY --from=builder /app/module-api/build/libs/*.jar app.jar

RUN chown app:app app.jar
USER app

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Duser.timezone=Asia/Seoul"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar app.jar"]