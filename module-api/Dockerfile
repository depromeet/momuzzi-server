FROM eclipse-temurin:21-jdk-jammy

ARG JAR_FILE=module-api/build/libs/*.jar

# GPG 키 업데이트 및 저장소 업데이트 (서명 오류 방지)
RUN apt-get update --allow-releaseinfo-change || \
    (apt-get clean && \
     rm -rf /var/lib/apt/lists/* && \
     apt-get update --allow-releaseinfo-change) && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

COPY ${JAR_FILE} app.jar

RUN ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && echo "Asia/Seoul" > /etc/timezone

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "app.jar"]