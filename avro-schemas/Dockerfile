FROM gradle:8.14.0-jdk24 AS builder
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle clean publish -Pnexus_host=${NEXUS_HOST} -Pnexus_user=${NEXUS_ADMIN} -Pnexus_password=${NEXUS_PASSWORD} --no-daemon