FROM openjdk:11-jre-slim

RUN apt update && apt install -y --no-install-recommends fontconfig && rm -rf /var/lib/apt/lists/*

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /usr/local/app.jar
ENV JVM_OPTION="-Djava.awt.headless=true"
ENV TZ=Asia/Shanghai JAVA_OPTS="-Duser.timezone=Asia/Shanghai -Djava.security.egd=file:/dev/urandom"
EXPOSE 80
ENTRYPOINT java ${JAVA_OPTS} ${JVM_OPTION} -jar /usr/local/app.jar