FROM eclipse-temurin:21-jre
ARG JAR_FILE=target/apiGateWay-0.0.1-SNAPSHOT.jar

RUN apt-get update && apt-get install -y curl netcat && rm -rf /var/lib/apt/lists/*
COPY ${JAR_FILE} /app/app.jar
WORKDIR /app
ENTRYPOINT ["java","-jar","/app/app.jar"]
EXPOSE 8080
