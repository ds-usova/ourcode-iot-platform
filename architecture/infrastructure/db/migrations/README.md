# Database Migrations

This directory contains Flyway database migrations for the IoT platform services.

## Structure

```
migrations/
└── router/                             # Migrations for the Router Manager service schema
    └── V1__init_router_schema.sql
```

## Flyway Migration Naming Convention

Migration files should follow the Flyway naming convention: 
- **V{version}__{description}.sql** - Versioned migrations (e.g., V1__init_router_schema.sql)
