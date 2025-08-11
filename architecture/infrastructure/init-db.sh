#!/bin/bash
set -e

initialize_schema() {
    local db_user=$1
    local db_password=$2
    local db_schema=$3

    echo "Initializing database schema..."
    echo "User: $db_user, Schema: $db_schema"

    # Create user and schema in the main database
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        -- Create user with encrypted password
        CREATE USER "$db_user" WITH ENCRYPTED PASSWORD '$db_password';

        -- Create schema
        CREATE SCHEMA IF NOT EXISTS "$db_schema";

        -- Grant schema privileges to user
        GRANT ALL PRIVILEGES ON SCHEMA "$db_schema" TO "$db_user";

        -- Grant privileges on future tables in the schema
        ALTER DEFAULT PRIVILEGES IN SCHEMA "$db_schema"
        GRANT ALL PRIVILEGES ON TABLES TO "$db_user";

        -- Grant privileges on future sequences in the schema
        ALTER DEFAULT PRIVILEGES IN SCHEMA "$db_schema"
        GRANT ALL PRIVILEGES ON SEQUENCES TO "$db_user";
EOSQL

    echo "Schema initialization completed successfully for $db_schema!"
}

# Create keycloak user and schema
KEYCLOAK_DB_USER=${KEYCLOAK_DB_USER:-keycloak_user}
KEYCLOAK_DB_PASSWORD=${KEYCLOAK_DB_PASSWORD:-keycloak_pass}
KEYCLOAK_DB_SCHEMA=${KEYCLOAK_DB_SCHEMA:-keycloak}

initialize_schema "$KEYCLOAK_DB_USER" "$KEYCLOAK_DB_PASSWORD" "$KEYCLOAK_DB_SCHEMA"

# Create camunda user and schema
CAMUNDA_DB_USER=${CAMUNDA_DB_USER:-camunda_user}
CAMUNDA_DB_PASSWORD=${CAMUNDA_DB_PASSWORD:-camunda_pass}
CAMUNDA_DB_SCHEMA=${CAMUNDA_DB_SCHEMA:-camunda}

initialize_schema "$CAMUNDA_DB_USER" "$CAMUNDA_DB_PASSWORD" "$CAMUNDA_DB_SCHEMA"
