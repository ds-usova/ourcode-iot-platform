FROM gradle:8.14.0-jdk24 AS builder
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle clean publish -Partifactory_host=${ARTIFACTORY_HOST} --no-daemon