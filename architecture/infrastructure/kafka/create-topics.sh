#!/bin/bash
set -e

BOOTSTRAP_SERVER=kafka:9092
KAFKA_BIN=/opt/kafka/bin

echo "Creating Kafka topics on $BOOTSTRAP_SERVER ..."
echo "======================================="

$KAFKA_BIN/kafka-topics.sh --create --if-not-exists \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --partitions 1 \
  --replication-factor 1 \
  --config cleanup.policy=compact \
  --topic _schemas

$KAFKA_BIN/kafka-topics.sh --create --if-not-exists \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --replication-factor 1 \
  --partitions 3 \
  --topic events

$KAFKA_BIN/kafka-topics.sh --create --if-not-exists \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --replication-factor 1 \
  --partitions 3 \
  --topic events-dlt

$KAFKA_BIN/kafka-topics.sh --create --if-not-exists \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --replication-factor 1 \
  --partitions 3 \
  --topic device-ids\

$KAFKA_BIN/kafka-topics.sh --create --if-not-exists \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --replication-factor 1 \
  --partitions 3 \
  --topic device-ids-dlt

echo "======================================="
echo "All topics created (or already existed)."
