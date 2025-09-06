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
