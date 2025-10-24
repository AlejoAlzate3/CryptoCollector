FROM eclipse-temurin:21-jre
ARG JAR_FILE=target/configServer-0.0.1-SNAPSHOT.jar

RUN apt-get update && apt-get install -y curl netcat-openbsd && rm -rf /var/lib/apt/lists/*
COPY ${JAR_FILE} /app/app.jar
COPY src/main/resources/config /app/config
WORKDIR /app
ENTRYPOINT ["java","-jar","/app/app.jar"]
EXPOSE 8888
