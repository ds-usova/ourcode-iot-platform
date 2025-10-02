#!/bin/bash

set -e

echo "Configuring PostgreSQL master for streaming replication..."

# Create replication user
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER replicator WITH REPLICATION ENCRYPTED PASSWORD '$POSTGRES_REPLICATION_PASSWORD';
EOSQL

# Configure PostgreSQL for replication
cat >> "$PGDATA/postgresql.conf" <<EOF

# Master configuration for streaming replication
wal_level = replica
max_wal_senders = 3
max_replication_slots = 3
wal_keep_size = 64MB

# Synchronous replication settings - RELAXED FOR DEVELOPMENT
synchronous_standby_names = ''

# Logging settings
log_replication_commands = on
wal_log_hints = on

# Archive settings
archive_mode = on
archive_command = '/bin/true'
EOF

# Configure client authentication for replication
cat >> "$PGDATA/pg_hba.conf" <<EOF

# Replication connections
host replication replicator 0.0.0.0/0 md5
EOF

echo "Master configuration completed!"

echo "Reloading PostgreSQL configuration..."
pg_ctl reload -D "$PGDATA"

echo "PostgreSQL master is ready for replication!"
