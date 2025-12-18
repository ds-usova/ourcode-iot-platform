FROM gradle:8.14.0-jdk24 AS builder
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle clean build -Pnexus_host=${NEXUS_HOST} -Pnexus_user=${NEXUS_ADMIN} -Pnexus_password=${NEXUS_PASSWORD} -x test --no-daemon

FROM eclipse-temurin:24-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]