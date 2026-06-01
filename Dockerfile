# Runtime stage
FROM eclipse-temurin:11-jre
WORKDIR /app
RUN apt-get update && \
    apt-get install -y --no-install-recommends fontconfig fonts-noto-cjk && \
    rm -rf /var/lib/apt/lists/*

COPY target/*.jar app.jar
ENV JAVA_OPTS="-Djava.awt.headless=true -Duser.timezone=Asia/Shanghai -Djava.security.egd=file:/dev/urandom"
ENV TZ=Asia/Shanghai
EXPOSE 8084
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]