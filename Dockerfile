# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp clean package

FROM eclipse-temurin:21-jre

ARG APP_UID=10001

WORKDIR /app

RUN groupadd --system --gid "${APP_UID}" spring \
    && useradd --system --uid "${APP_UID}" --gid spring \
        --home-dir /app --shell /usr/sbin/nologin spring \
    && mkdir -p /app/logs \
    && chown -R spring:spring /app

COPY --from=build --chown=spring:spring \
    /workspace/target/prime-0.0.1-SNAPSHOT.jar /app/app.jar

USER spring:spring

EXPOSE 8080

VOLUME ["/app/logs"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
