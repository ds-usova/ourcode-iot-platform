#!/bin/bash
set -e

# shard 1: public schema has been used, since sharding sphere doesn't support schemas yet
# https://github.com/apache/shardingsphere/issues/36056
# https://github.com/apache/shardingsphere/pull/35022
/flyway/flyway -url=jdbc:postgresql://postgres_shard_0:5432/"$DB_NAME" -user="$DB_USER" -password="$DB_PASSWORD" -locations=filesystem:/flyway/migrations migrate

# shard 2
/flyway/flyway -url=jdbc:postgresql://postgres_shard_1:5432/"$DB_NAME" -user="$DB_USER" -password="$DB_PASSWORD" -locations=filesystem:/flyway/migrations migrate

echo "All shard migrations complete"