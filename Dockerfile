#DockerFile

#JDK 17
FROM eclipse-temurin:17-jre-jammy

RUN addgroup --system cofshop && adduser --system --ingroup cofshop cofshop
WORKDIR /app

#인자 설정
ARG JAR_FILE=build/libs/*.jar

#jar 파일 복제
COPY ${JAR_FILE} /app/app.jar
RUN chown cofshop:cofshop /app/app.jar

ENV SPRING_PROFILES_ACTIVE=prod
USER cofshop
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
