#!/bin/bash
set -e

# shard 1
/flyway/flyway -url=jdbc:postgresql://postgres_shard_0:5432/"$DB_NAME" -user="$DB_USER" -password="$DB_PASSWORD" -locations=filesystem:/flyway/migrations -schemas="$DB_NAME" migrate

# shard 2
/flyway/flyway -url=jdbc:postgresql://postgres_shard_1:5432/"$DB_NAME" -user="$DB_USER" -password="$DB_PASSWORD" -locations=filesystem:/flyway/migrations -schemas="$DB_NAME" migrate

echo "All shard migrations complete"