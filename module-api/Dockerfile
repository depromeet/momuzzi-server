FROM eclipse-temurin:21-jdk-jammy

ARG JAR_FILE=module-api/build/libs/*.jar

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*


COPY ${JAR_FILE} app.jar

RUN ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && echo "Asia/Seoul" > /etc/timezone

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "app.jar"]