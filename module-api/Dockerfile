FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# 시스템 패키지 설치
RUN apt-get update && apt-get install -y unzip curl && rm -rf /var/lib/apt/lists/*

COPY . .

RUN chmod +x ./gradlew

# 빌드 실행
RUN ./gradlew :module-api:bootJar -x test --no-daemon

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