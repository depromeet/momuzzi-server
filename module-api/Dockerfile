FROM eclipse-temurin:21-jre-alpine

ARG JAR_FILE=module-api/build/libs/*.jar

# Alpine은 apk 사용 (GPG 서명 문제 없음)
RUN apk add --no-cache curl tzdata

COPY ${JAR_FILE} app.jar

# 타임존 설정
ENV TZ=Asia/Seoul
RUN ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "app.jar"]