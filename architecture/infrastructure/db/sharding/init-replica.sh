#!/bin/bash
set -e

echo "Configuring PostgreSQL replica for streaming replication..."

# Wait for master to be ready
until pg_isready -h postgres_shard_0 -p 5432 -U "$POSTGRES_USER"; do
    echo "Waiting for master to be ready..."
    sleep 2
done

# Stop PostgreSQL if running
pg_ctl stop -D "$PGDATA" -m fast || true

# Remove existing data directory contents
rm -rf "$PGDATA"/*

# Create base backup from master
# -v = verbose output
# -P = show progress
# -R = writes a replication configuration file (standby.signal)
# -W = prompt for password
echo "Creating base backup from master..."
PGPASSWORD="${POSTGRES_REPLICATION_PASSWORD}" pg_basebackup \
    -h postgres_shard_0 \
    -D "$PGDATA" \
    -U replicator \
    -v \
    -P \
    -R \
    -W

# Configure replica-specific settings
cat >> "$PGDATA/postgresql.conf" <<EOF

# Replica-specific configuration
hot_standby = on
max_standby_streaming_delay = 30s
max_standby_archive_delay = 30s
wal_receiver_status_interval = 10s
hot_standby_feedback = on

# Recovery and replication settings
restore_command = '/bin/true'
recovery_target_timeline = 'latest'

# Connection settings for better reliability
wal_receiver_timeout = 60s
wal_retrieve_retry_interval = 5s

# Replica connection info with better error handling
primary_conninfo = 'host=postgres_shard_0 port=5432 user=replicator password=${POSTGRES_REPLICATION_PASSWORD} application_name=shard_0_replica connect_timeout=10'
EOF

# Set permissions
chown -R postgres:postgres "$PGDATA"
chmod 700 "$PGDATA"

echo "Replica configuration completed successfully!"

echo "Starting PostgreSQL replica..."
exec postgres