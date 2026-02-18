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
COPY --from=builder /build/${SERVICE_MODULE}/target/${SERVICE_MODULE}-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
