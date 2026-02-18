# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml .
COPY common ./common
COPY order-service ./order-service
COPY product-service ./product-service
COPY notification-service ./notification-service

ARG SERVICE_MODULE
RUN test -n "$SERVICE_MODULE"
RUN mvn -B -pl ${SERVICE_MODULE} -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

ARG SERVICE_MODULE
ARG APP_PORT=8080

COPY --from=builder /build/${SERVICE_MODULE}/target/*.jar /tmp/
RUN set -eux; \
    jar_file="$(find /tmp -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)"; \
    test -n "$jar_file"; \
    mv "$jar_file" /app/app.jar; \
    rm -f /tmp/*.jar

EXPOSE ${APP_PORT}
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
