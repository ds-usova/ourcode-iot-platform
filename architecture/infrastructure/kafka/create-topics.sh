#!/bin/bash
set -e

BOOTSTRAP_SERVER=kafka:9092
KAFKA_BIN=/opt/kafka/bin

echo "Creating Kafka topics on $BOOTSTRAP_SERVER ..."

$KAFKA_BIN/kafka-topics.sh --create --if-not-exists \
  --bootstrap-server "$BOOTSTRAP_SERVER" \
  --replication-factor 1 \
  --partitions 3 \
  --topic events
echo "Topic 'events' created (or already existed)."

echo "All topics created (or already existed)."
