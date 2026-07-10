# MySQL MCP Server

A Spring Boot application that allows AI assistants to securely interact with user-configured MySQL databases through the **Model Context Protocol (MCP)**.

## Architecture Overview

```
┌─────────────────┐          ┌──────────────────┐
│  REST APIs      │          │  MCP Tools        │
│  (Management)   │          │  (DB Operations)  │
└────────┬────────┘          └────────┬─────────┘
         │                            │
         └────────────┬───────────────┘
                      ▼
            ┌──────────────────┐
            │   Service Layer  │
            │  (Auth, API Key, │
            │   Permission,    │
            │   Audit, DB Exec)│
            └────────┬─────────┘
                     │
           ┌─────────┴──────────┐
           ▼                    ▼
   ┌──────────────┐   ┌─────────────────┐
   │ Application  │   │  Dynamic JDBC   │
   │ DB (JPA)     │   │  → User's MySQL │
   └──────────────┘   └─────────────────┘
```

## Project Structure — Where to Look

### `com.mysqlmcp`

| Package | Purpose | Key Files |
|---------|---------|-----------|
| **`config/`** | App configuration & properties | `MysqlMcpProperties.java` — safety limits (max rows, timeouts) |
| **`controller/`** | REST API endpoints | `DatabaseConnectionController.java`, `ApiKeyController.java`, `AuditLogController.java`, `AuthController.java` |
| **`mcp/`** | MCP tool implementations | `DatabaseMetadataTools.java` (show_tables, describe_table), `DatabaseQueryTools.java` (execute_select, explain_query) |
| **`service/`** | Business logic | `DatabaseExecutionService.java` — dynamic DB execution, `ApiKeyValidationService.java` — API key hashing/validation, `AuditLogService.java` — audit recording |
| **`database/`** | Dynamic DB connection infra | `DynamicJdbcTemplateProvider.java`, `JdbcUrlBuilder.java`, `DatabaseCredentialEncryptor.java` |
| **`security/`** | Authentication | `ApiKeyAuthenticationFilter.java` — extracts Bearer token, `SecurityConfig.java` — endpoint protection |
| **`entity/`** | JPA entities (application DB) | `User`, `DatabaseConnection`, `ApiKey`, `ApiKeyPermission`, `AuditLog` |
| **`repository/`** | Spring Data JPA repositories | 5 interfaces with custom queries |
| **`dto/`** | Data transfer objects | `DatabaseCredentials`, `DatabaseAccessContext`, request records |
| **`enums/`** | Enumerations | `DatabasePermission` (SHOW_TABLES, SELECT, etc.), `AuditSourceType` (MCP, REST, SYSTEM) |
| **`exception/`** | Custom exceptions + handler | `GlobalExceptionHandler.java` — structured JSON error responses |

### Resources

| File | Purpose |
|------|---------|
| `application.yml` | Database config, Flyway, safety limits, encryption key |
| `db/migration/V1__init_schema.sql` | Schema for the 5 application tables |

## How It Works

### Two Databases
1. **Application DB** (`mysql_mcp`) — stores users, DB configs, API keys, permissions, audit logs via JPA
2. **Target DB** (any MySQL) — user's external database, accessed dynamically via `JdbcTemplate`

### API Key Flow
```
Client sends: Authorization: Bearer mcp_live_xxxxxxxxx
         ↓
ApiKeyAuthenticationFilter extracts the token
         ↓
ApiKeyValidationService hashes it (SHA-256), looks up in DB
         ↓
Resolves DatabaseConnection + permissions → DatabaseAccessContext
         ↓
MCP tools use this context to check permission and execute queries
```

### MCP Tools

| Tool | Required Permission | Returns |
|------|--------------------|---------|
| `show_tables` | SHOW_TABLES | List of table names |
| `describe_table` | DESCRIBE_TABLE | Column metadata |
| `execute_select` | SELECT | Results with row limit enforcement |
| `explain_query` | EXPLAIN | MySQL EXPLAIN output |

### Security
- **API keys** are SHA-256 hashed before storage (raw key shown once at creation)
- **DB passwords** are AES-encrypted (decrypted only at query time)
- **User passwords** must be hashed
- All sensitive data is excluded from audit logs

### Audit Logging
Every MCP operation records: who (API key), what (tool, input), result (success/failure, rows, time, error). Sensitive data is never logged.

### Safety Limits (configurable in `application.yml`)
- `mysql-mcp.execution.max-rows=1000` — max rows returned
- `mysql-mcp.execution.query-timeout-seconds=30` — query timeout
- `mysql-mcp.execution.max-query-length=10000` — max query length

## REST API Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/auth/register` | User registration (TODO) |
| POST | `/api/auth/login` | User login (TODO) |
| POST | `/api/database-connections` | Add a target DB config |
| GET | `/api/database-connections` | List DB configs |
| POST | `/api/database-connections/test` | Test DB connectivity |
| POST | `/api/api-keys` | Generate API key (returns full key once) |
| GET | `/api/api-keys` | List API keys for a DB |
| PATCH | `/api/api-keys/{id}/status` | Enable/disable API key |
| GET | `/api/audit-logs` | View audit logs |

## Getting Started

1. **Prerequisites**: Java 21, MySQL (both application DB + target DB)
2. **Create application database**:
   ```sql
   CREATE DATABASE mysql_mcp;
   ```
3. **Update** `src/main/resources/application.yml` with your MySQL credentials and encryption key
4. **Build & run**:
   ```bash
   mvn clean compile
   mvn spring-boot:run
   ```
5. The app will auto-create tables via Flyway on first startup

## Development Strategy

This project is designed for 2 parallel developers. See `PROJECT_OVERVIEW.md` for the full phased plan.