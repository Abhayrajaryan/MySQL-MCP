# MySQL MCP Server — Project Overview

## 1. Project Overview

### 1.1 Project Name
MySQL MCP Server

### 1.2 Objective
The goal of this project is to build a single Spring Boot application that allows AI assistants to securely interact with user-configured MySQL databases through the Model Context Protocol (MCP).

A user will be able to:
- Register with the application.
- Add one or more MySQL database configurations.
- Generate API keys associated with a specific database configuration.
- Assign database-operation permissions to each API key.
- Connect an MCP-compatible AI assistant using the generated API key.
- Allow the AI assistant to perform only the database operations permitted for that API key.
- View audit logs showing what was requested, by whom, what action was performed, and the result.

The application will contain both:
- **REST APIs** for the future web UI and management operations.
- **MCP tools** for AI-driven database operations.

Both interfaces will exist within a single Spring Boot application and share the same service, security, repository, and database infrastructure.

---

## 2. High-Level Architecture

```
                         ONE SPRING BOOT APPLICATION
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│   ┌───────────────────────┐       ┌───────────────────────────┐  │
│   │    REST API Layer     │       │      MCP Tool Layer       │  │
│   │                       │       │                           │  │
│   │  User Registration    │       │  showTables              │  │
│   │  Login                │       │  describeTable           │  │
│   │  DB Configuration     │       │  executeSelect           │  │
│   │  API Key Management   │       │  executeInsert           │  │
│   │  Audit Log Viewing    │       │  executeUpdate           │  │
│   │  Test Connection      │       │  executeDelete           │  │
│   │                       │       │  explainQuery            │  │
│   └───────────┬───────────┘       └────────────┬──────────────┘  │
│               │                                │                 │
│               └──────────────┬─────────────────┘                 │
│                              ▼                                   │
│                   ┌───────────────────────┐                      │
│                   │     Service Layer     │                      │
│                   │                       │                      │
│                   │  Authentication       │                      │
│                   │  API Key Validation   │                      │
│                   │  Permission Checking  │                      │
│                   │  Audit Logging        │                      │
│                   │  DB Execution         │                      │
│                   └───────────┬───────────┘                      │
│                               │                                  │
│                  ┌────────────┴────────────┐                     │
│                  ▼                         ▼                     │
│        ┌───────────────────┐    ┌──────────────────────┐        │
│        │  Application DB   │    │ Dynamic JdbcTemplate │        │
│        │                   │    │                      │        │
│        │ users             │    │ Created using user's │        │
│        │ db_connections    │    │ dynamic DB config    │        │
│        │ api_keys          │    └──────────┬───────────┘        │
│        │ permissions       │               │                    │
│        │ audit_logs        │               ▼                    │
│        └───────────────────┘      User's External MySQL DB      │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 3. Core Architectural Decisions

### 3.1 Single Application
REST APIs and MCP tools will be implemented in the same Spring Boot application. No separate microservices.

### 3.2 REST APIs
REST endpoints will primarily support the UI and application-management operations:
- Registration
- Login
- Database configuration management
- API key management
- Audit-log viewing
- Testing database connectivity

AI-driven database execution will **not** be exposed through normal REST endpoints.

### 3.3 MCP Tools
All AI-driven database operations will be exposed as MCP tools:
- `show_tables`
- `describe_table`
- `execute_select`
- `explain_query`
- `execute_insert`
- `execute_update`
- `execute_delete`

### 3.4 Dynamic Database Connections
The application will dynamically connect to a user's external MySQL database based on the database configuration associated with the provided API key.

```
Database Configuration
        ↓
DriverManagerDataSource
        ↓
JdbcTemplate
        ↓
Target MySQL Database
```

Connection pooling with HikariCP can be introduced later.

### 3.5 Application Database vs Target Database

**Application Database** — Owned by the MySQL MCP application itself:
- `users`
- `database_connections`
- `api_keys`
- `api_key_permissions`
- `audit_logs`

Spring Data JPA will be used for this database.

**Target Database** — An external MySQL database configured by a user. The application knows nothing about its schema in advance. Dynamic `JdbcTemplate` instances will be used to interact with target databases.

### 3.6 Authentication
Two authentication mechanisms:
- **REST/UI Authentication** → JWT or session-based; identifies a registered user.
- **MCP Authentication** → API key in HTTP `Authorization` header; identifies an API key, resolves database configuration and permissions.

Expected MCP authentication header: `Authorization: Bearer mcp_live_xxxxxxxxx`

### 3.7 API-Key Storage
A complete API key is returned only once when generated. The application database stores:
- `key_prefix`
- `key_hash`

The raw API key must **never** be stored.

### 3.8 Database Password Storage
Target database passwords must be **encrypted** (not hashed), because the application needs to decrypt them to establish database connections.

| Data            | Method    |
|-----------------|-----------|
| User Password   | Hash      |
| API Key         | Hash      |
| DB Password     | Encrypt   |

### 3.9 Audit Logging
The system will log:
- Who made the request?
- Which API key was used?
- Which MCP tool was called?
- What input was received?
- Which database configuration was targeted?
- What operation was performed?
- Was it successful?
- How many rows were returned or affected?
- How long did execution take?
- What error occurred, if any?
- When did it happen?

The following must **never** be logged:
- Raw API keys
- Raw user passwords
- Raw database passwords
- Decrypted database credentials
- Authorization headers

---

## 4. Database Schema

### 4.1 User
```
users
-----------------------------------
id                  BIGINT PK
email               VARCHAR UNIQUE
password_hash       VARCHAR
created_at          TIMESTAMP
updated_at          TIMESTAMP
```

### 4.2 Database Connection
```
database_connections
-----------------------------------
id                  BIGINT PK
user_id             BIGINT FK
name                VARCHAR
host                VARCHAR
port                INT
database_name       VARCHAR
db_username         VARCHAR
encrypted_password  TEXT
is_active           BOOLEAN
created_at          TIMESTAMP
updated_at          TIMESTAMP
```

The JDBC URL is constructed internally: `jdbc:mysql://{host}:{port}/{database_name}`

### 4.3 API Key
```
api_keys
-----------------------------------
id                       BIGINT PK
database_connection_id   BIGINT FK
name                     VARCHAR
key_prefix               VARCHAR
key_hash                 VARCHAR
is_active                BOOLEAN
expires_at               TIMESTAMP NULL
created_at               TIMESTAMP
last_used_at             TIMESTAMP NULL
```

One database configuration may have multiple API keys (e.g., Read Only Key, Analytics Key, Admin Key).

### 4.4 API-Key Permissions
```
api_key_permissions
-----------------------------------
id                  BIGINT PK
api_key_id          BIGINT FK
permission          VARCHAR
```

Initial permission enum:
```java
public enum DatabasePermission {
    SHOW_TABLES,
    DESCRIBE_TABLE,
    SELECT,
    EXPLAIN,
    INSERT,
    UPDATE,
    DELETE,
    CREATE_TABLE,
    ALTER_TABLE,
    DROP_TABLE
}
```

### 4.5 Audit Log
```
audit_logs
-----------------------------------
id                       BIGINT PK
api_key_id               BIGINT FK NULL
database_connection_id   BIGINT FK NULL
source_type              VARCHAR
operation                VARCHAR
request_payload          TEXT
response_summary         TEXT
success                  BOOLEAN
rows_affected            BIGINT NULL
execution_time_ms        BIGINT
error_code               VARCHAR NULL
error_message            TEXT NULL
created_at               TIMESTAMP
```

For database execution, a **summary** is stored instead of the complete raw response (which could be millions of rows or contain sensitive data).

---

## 5. Package Structure

```
com.mysqlmcp
│
├── controller
│   ├── AuthController
│   ├── DatabaseConnectionController
│   ├── ApiKeyController
│   └── AuditLogController
│
├── mcp
│   ├── DatabaseMetadataTools
│   ├── DatabaseQueryTools
│   └── DatabaseModificationTools
│
├── service
│   ├── AuthService
│   ├── DatabaseConnectionService
│   ├── ApiKeyService
│   ├── ApiKeyValidationService
│   ├── PermissionService
│   ├── AuditLogService
│   └── DatabaseExecutionService
│
├── database
│   ├── DynamicJdbcTemplateProvider
│   ├── JdbcUrlBuilder
│   └── DatabaseCredentialEncryptor
│
├── security
│   ├── ApiKeyAuthenticationFilter
│   ├── ApiKeyAuthentication
│   └── SecurityConfig
│
├── entity
│   ├── User
│   ├── DatabaseConnection
│   ├── ApiKey
│   ├── ApiKeyPermission
│   └── AuditLog
│
├── repository
│   ├── UserRepository
│   ├── DatabaseConnectionRepository
│   ├── ApiKeyRepository
│   ├── ApiKeyPermissionRepository
│   └── AuditLogRepository
│
├── dto
│   ├── request
│   └── response
│
├── exception
│   ├── GlobalExceptionHandler
│   ├── InvalidApiKeyException
│   ├── DatabaseConnectionException
│   ├── PermissionDeniedException
│   ├── QueryTimeoutException
│   └── InvalidQueryException
│
├── enums
│   ├── DatabasePermission
│   ├── AuditSourceType
│   └── AuditOperationStatus
│
├── config
│
└── MysqlMcpApplication
```

---

## 6. Request Flow (MCP Database Operations)

Every MCP database operation should conceptually follow this flow:

```
MCP Tool Call
      ↓
Extract API key from Authorization header
      ↓
Hash provided API key
      ↓
Find matching active API key
      ↓
Check expiration
      ↓
Load associated DatabaseConnection
      ↓
Check required permission
      ↓
Create audit-log request entry/context
      ↓
Decrypt target database password
      ↓
Create dynamic JdbcTemplate
      ↓
Execute operation with limits/timeouts
      ↓
Record result in audit log
      ↓
Return structured response
```

Common authentication, permission, audit, and execution infrastructure should be reusable across all tools.

---

## 7. MCP Tools — Initial Contract

### 7.1 `show_tables`
- **Required permission:** `SHOW_TABLES`
- **Input:** No tool-specific input
- **Output:**
  ```json
  {
    "tables": ["employees", "departments", "attendance"],
    "count": 3
  }
  ```

### 7.2 `describe_table`
- **Required permission:** `DESCRIBE_TABLE`
- **Input:** `{ "tableName": "employees" }`
- **Output:**
  ```json
  {
    "tableName": "employees",
    "columns": [
      { "name": "id", "type": "BIGINT", "nullable": false, "primaryKey": true }
    ]
  }
  ```

### 7.3 `execute_select`
- **Required permission:** `SELECT`
- **Input:** `{ "query": "SELECT id, name FROM employees LIMIT 10" }`
- **Output:**
  ```json
  {
    "columns": ["id", "name"],
    "rows": [{ "id": 1, "name": "Alice" }],
    "returnedRows": 1,
    "truncated": false,
    "executionTimeMs": 15
  }
  ```

### 7.4 `explain_query`
- **Required permission:** `EXPLAIN`
- **Input:** `{ "query": "SELECT * FROM employees WHERE email = 'test@example.com'" }`
- **Output:** Structured MySQL EXPLAIN information.

### 7.5 Mutation Tools (Later Phase)
- `execute_insert`
- `execute_update`
- `execute_delete`
- `create_table`
- `alter_table`
- `drop_table`

Read operations should be completed and stabilized first.

---

## 8. Global Safety Limits

Configurable limits for the initial implementation:

```properties
mysql-mcp.execution.max-rows=1000
mysql-mcp.execution.query-timeout-seconds=30
mysql-mcp.connection.timeout-seconds=10
mysql-mcp.execution.max-query-length=10000
```

A SELECT result must include truncation information when the limit is hit:
```json
{
  "returnedRows": 1000,
  "truncated": true
}
```

---

## 9. Global Exception Handling

Suggested error model:
```json
{
  "code": "PERMISSION_DENIED",
  "message": "This API key does not have permission to execute SELECT operations.",
  "timestamp": "2026-07-10T15:30:00Z"
}
```

**Error codes:**
- `INVALID_API_KEY`
- `API_KEY_EXPIRED`
- `API_KEY_DISABLED`
- `PERMISSION_DENIED`
- `DATABASE_UNREACHABLE`
- `INVALID_DATABASE_CREDENTIALS`
- `DATABASE_CONNECTION_TIMEOUT`
- `INVALID_SQL`
- `QUERY_TIMEOUT`
- `QUERY_EXECUTION_FAILED`
- `RESULT_TOO_LARGE`
- `INTERNAL_ERROR`

Raw internal exceptions and sensitive connection information must **not** be exposed to clients.

---

## 10. Development Phases (2-Developer Team)

### Phase 0 — Project Bootstrap (Shared)
Create Spring Boot project, add dependencies, configure application database, agree on package structure, create shared enums and response contracts, create initial migration, push stable base branch.

### Phase 1 — Core Foundation (Parallel)
- **Developer A:** Entities, repositories, migrations, enums, DTO contracts.
- **Developer B:** DynamicJdbcTemplateProvider, JdbcUrlBuilder, DatabaseCredentialEncryptor, dynamic connection creation, connection testing, basic JdbcTemplate query execution.

### Phase 2 — API-Key Authentication & Database Execution (Parallel)
- **Developer A:** API-key generation, hashing, validation, expiration, permission resolution, DatabaseAccessContext.
- **Developer B:** showTables(), describeTable(), executeSelect(), explainQuery(), result mapping, limit enforcement.

### Phase 3 — MCP Integration & Audit Logging (Parallel)
- **Developer A:** MCP server transport configuration, expose MCP tools, wire to service layer.
- **Developer B:** Audit request creation, success/failure logging, execution-time measurement, response summary generation, sensitive-data redaction.

### Phase 4 — REST Management APIs (Parallel)
- **Developer A:** Database configuration CRUD endpoints + test connection endpoint.
- **Developer B:** API-key CRUD + status management + audit log viewing endpoints.

### Phase 5 — User Registration & Authentication
User registration, login, password hashing, JWT/session authentication, user ownership validation, protected REST endpoints.

### Phase 6 — Mutation Operations
execute_insert, execute_update, execute_delete, create_table, alter_table, drop_table.

### Phase 7 — Dynamic Connection Pooling (Future)
HikariCP pools managed per database configuration. Not needed for MVP unless performance testing proves otherwise.

### Phase 8 — Frontend
Web UI consuming REST APIs (login, registration, dashboard, database connections, API keys, audit logs).

---

## 11. Definition of MVP

The project is considered a functional MVP when:

- [x] A database configuration can be stored securely.
- [x] An API key can be generated for that database.
- [x] Different permissions can be assigned to an API key.
- [x] An MCP client can authenticate using that API key.
- [x] The API key resolves to the correct target database.
- [x] The server dynamically connects to that database.
- [x] The MCP client can call `show_tables`.
- [x] The MCP client can call `describe_table`.
- [x] The MCP client can execute permitted `SELECT` queries.
- [x] Unauthorized operations are rejected.
- [x] Query results have configurable limits.
- [x] Every operation is audited.
- [x] Sensitive credentials and raw API keys are never logged.
- [x] Global exception handling returns structured, safe errors.

---

## 12. First Milestone

1. Start Spring Boot application.
2. Connect to application MySQL database.
3. Store one `DatabaseConnection` record.
4. Read that configuration.
5. Decrypt its password.
6. Create `DynamicJdbcTemplate`.
7. Connect to the external target database.
8. Execute `SHOW TABLES`.
9. Return the result.

Once this works, the most fundamental technical assumption behind the project is validated. Everything else — API keys, permissions, MCP tools, audit logging, authentication, and UI — builds on that foundation.