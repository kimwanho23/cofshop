#DockerFile

#JDK 17
FROM openjdk:17-jdk-slim

#인자 설정
ARG JAR_FILE=build/libs/*.jar

#jar 파일 복제
COPY ${JAR_FILE} app.jar


ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
