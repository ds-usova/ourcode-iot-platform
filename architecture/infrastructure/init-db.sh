#!/bin/bash
set -e

KEYCLOAK_DB_USER=${KEYCLOAK_DB_USER:-keycloak_user}
KEYCLOAK_DB_PASSWORD=${KEYCLOAK_DB_PASSWORD:-keycloak_pass}
KEYCLOAK_DB_SCHEMA=${KEYCLOAK_DB_SCHEMA:-keycloak}

echo "Initializing database schema with environment variables..."
echo "User: $KEYCLOAK_DB_USER, Schema: $KEYCLOAK_DB_SCHEMA"

# Create Keycloak user and schema in the main database
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create user with encrypted password
    CREATE USER "$KEYCLOAK_DB_USER" WITH ENCRYPTED PASSWORD '$KEYCLOAK_DB_PASSWORD';

    -- Create schema
    CREATE SCHEMA IF NOT EXISTS "$KEYCLOAK_DB_SCHEMA";

    -- Grant schema privileges to Keycloak user
    GRANT ALL PRIVILEGES ON SCHEMA "$KEYCLOAK_DB_SCHEMA" TO "$KEYCLOAK_DB_USER";

    -- Grant privileges on future tables in the schema
    ALTER DEFAULT PRIVILEGES IN SCHEMA "$KEYCLOAK_DB_SCHEMA"
    GRANT ALL PRIVILEGES ON TABLES TO "$KEYCLOAK_DB_USER";

    -- Grant privileges on future sequences in the schema
    ALTER DEFAULT PRIVILEGES IN SCHEMA "$KEYCLOAK_DB_SCHEMA"
    GRANT ALL PRIVILEGES ON SEQUENCES TO "$KEYCLOAK_DB_USER";
EOSQL

echo "Schema initialization completed successfully!"
