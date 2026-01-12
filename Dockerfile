FROM openjdk:11-jdk-alpine
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /usr/local/app.jar
ENV JVM_OPTION=""
ENV TZ=Asia/Shanghai JAVA_OPTS="-Duser.timezone=Asia/Shanghai -Djava.security.egd=file:/dev/urandom"
EXPOSE 80
ENTRYPOINT java ${JAVA_OPTS} ${JVM_OPTION} -jar /usr/local/app.jar