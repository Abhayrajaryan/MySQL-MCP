# MySQL MCP Server — Project Overview

## 1. Project Overview

### 1.1 Project Name
MySQL MCP Server

### 1.2 Objective
The goal of this project is to build a single Spring Boot application that allows AI assistants to securely interact with user-configured MySQL databases through the Model Context Protocol (MCP).

This is a **single-user, self-hosted** application. There is no registration or multi-tenant user system — a person downloads the jar, runs it on their own server, and logs in with either a username/password they configured or a built-in default.

A user will be able to:
- Run the jar and log in using a configured (or default) username/password.
- Add one or more MySQL database configurations.
- Generate API keys associated with a specific database configuration.
- Assign database-operation permissions to each API key.
- Connect an MCP-compatible AI assistant using the generated API key.
- Allow the AI assistant to perform only the database operations permitted for that API key.
- View audit logs showing what was requested, by whom (which API key), what action was performed, and the result.

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
│   │  Login (single user)  │       │  showTables              │  │
│   │  DB Configuration     │       │  describeTable           │  │
│   │  API Key Management   │       │  executeSelect           │  │
│   │  Audit Log Viewing    │       │  executeInsert           │  │
│   │  Test Connection      │       │  executeUpdate           │  │
│   │                       │       │  executeDelete           │  │
│   │                       │       │  explainQuery            │  │
│   └───────────┬───────────┘       └────────────┬──────────────┘  │
│               │                                │                 │
│               └──────────────┬─────────────────┘                 │
│                              ▼                                   │
│                   ┌───────────────────────┐                      │
│                   │     Service Layer     │                      │
│                   │                       │                      │
│                   │  Single-User Auth     │                      │
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
│        │ db_connections    │    │ Created using user's │        │
│        │ api_keys          │    │ dynamic DB config    │        │
│        │ permissions       │    └──────────┬───────────┘        │
│        │ audit_logs        │               │                    │
│        └───────────────────┘               ▼                    │
│                                    User's External MySQL DB      │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

Note: there is no `users` table. The application has exactly one operator, whose credentials are supplied via configuration, not stored as a database row.

---

## 3. Core Architectural Decisions

### 3.1 Single Application
REST APIs and MCP tools will be implemented in the same Spring Boot application. No separate microservices.

### 3.2 REST APIs
REST endpoints will primarily support the UI and application-management operations:
- Login (single configured user)
- Database configuration management
- API key management
- Audit-log viewing
- Testing database connectivity

There is no registration endpoint — the application ships with exactly one user, defined via configuration.

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
- `database_connections`
- `api_keys`
- `api_key_permissions`
- `audit_logs`

There is deliberately **no `users` table** and **no `user_id` foreign key** anywhere in the schema — since only one user ever exists per running instance, ownership modeling is unnecessary. That single user's identity is entirely defined by configuration (properties/environment variables), not persisted as a row.

Spring Data JPA will be used for this database.

**Target Database** — An external MySQL database configured by the user. The application knows nothing about its schema in advance. Dynamic `JdbcTemplate` instances will be used to interact with target databases.

### 3.6 Authentication

Two authentication mechanisms:

- **REST/UI Authentication** → Single-user, configuration-based login. There is no user table and no registration flow. The application defines default credentials, which can be overridden via `application.properties` or environment variables.

  ```properties
  # application.properties (defaults, safe for local/dev use only)
  mysql-mcp.auth.username=admin
  mysql-mcp.auth.password=admin123
  ```

  These can be overridden without touching the properties file, using standard Spring Boot relaxed environment-variable binding:

  ```
  MYSQL_MCP_AUTH_USERNAME=myname
  MYSQL_MCP_AUTH_PASSWORD=my-strong-password
  ```

  On login, the submitted username/password is compared against the configured (or default) values. On success, a JWT or session is issued — it identifies "the operator," not a database row, since there is only ever one.

  It is strongly recommended the application log a warning at startup if the default credentials are still in effect, so operators know to override them.

- **MCP Authentication** → API key in HTTP `Authorization` header; identifies an API key, resolves database configuration and permissions.

Expected MCP authentication header: `Authorization: Bearer mcp_live_xxxxxxxxx`

### 3.7 API-Key Storage
A complete API key is returned only once when generated. The application database stores:
- `key_prefix`
- `key_hash`

The raw API key must **never** be stored.

### 3.8 Database Password Storage
Target database passwords must be **encrypted** (not hashed), because the application needs to decrypt them to establish database connections.

| Data                  | Method    |
|-----------------------|-----------|
| Single-User Password  | Configured (not stored in DB) |
| API Key               | Hash      |
| DB Password           | Encrypt   |

### 3.9 Audit Logging
The system will log:
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

Since there is only one user, "who made the request" is captured at the API-key level (and, for REST management actions, simply as "the operator") rather than via a user foreign key.

The following must **never** be logged:
- Raw API keys
- The configured single-user password
- Raw database passwords
- Decrypted database credentials
- Authorization headers

---

## 4. Database Schema

There is no `users` table. The single user's credentials live in configuration only (`application.properties` and/or environment variables), never in the application database.

### 4.1 Database Connection
```
database_connections
-----------------------------------
id                  BIGINT PK
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

No `user_id` foreign key — every row in this application database belongs to the single operator by definition.

The JDBC URL is constructed internally: `jdbc:mysql://{host}:{port}/{database_name}`

### 4.2 API Key
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

### 4.3 API-Key Permissions
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

### 4.4 Audit Log
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
│   ├── AuthController              (login only — no registration)
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
│   ├── SingleUserAuthService        (validates against configured credentials)
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
│   ├── DatabaseConnection
│   ├── ApiKey
│   ├── ApiKeyPermission
│   └── AuditLog
│
├── repository
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
│   └── AppAuthProperties            (binds mysql-mcp.auth.username / .password)
│
└── MysqlMcpApplication
```

Note there is no `User` entity and no `UserRepository` — the single user is a configuration concept, not a persisted one.

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
- `INVALID_CREDENTIALS` (login with wrong username/password)
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

## 10. Development Phases (Solo Learner — ~39 Small Milestones)

Each phase introduces **one or two new concepts** and ends with something you can **run, test, and understand**. No phase depends on API keys, permissions, or audit logging until those are deliberately introduced later.

For every phase, ask yourself:
- **What did I build?**
- **Why is it needed?**
- **How does the request/data flow through my code?**
- **How can I prove that it works?**

---

### Stage 1 — Bootstrap & Application Database (Phases 1–5)

**Phase 1 — Bootstrap the Spring Boot application**
- Create Spring Boot project with: Spring Web, Spring Data JPA, MySQL Driver, Validation, Lombok.
- Create basic package structure (`entity`, `repository`, `service`, `controller`, `database`, `config`).
- Add `@SpringBootApplication` main class.
- **Learn:** What `@SpringBootApplication` does, component scanning, auto-configuration.
- **Prove it:** Start the application. It runs without errors (even if it can't connect to a database yet).

**Phase 2 — Connect to the application's own MySQL database**
- Create a local MySQL database (e.g., `mysql_mcp`).
- Configure `application.properties` with datasource URL, username, password, JPA settings.
- Let Spring Boot auto-configure the primary datasource.
- **Learn:** What a DataSource is, JDBC URL meaning, Spring Data JPA → Hibernate → JDBC → MySQL relationship.
- **Prove it:** Application starts and connects to MySQL. Logs show "HHH000400: Using dialect" or similar.

**Phase 3 — Create the first entity: `DatabaseConnection`**
- Create entity with fields: `id`, `name`, `host`, `port`, `databaseName`, `dbUsername`, `password` (plain text for now), `isActive`, `createdAt`, `updatedAt`.
- No `userId` field — this application has only one user.
- Use `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column` annotations.
- **Learn:** Entity lifecycle, how Java objects map to database rows.
- **Prove it:** Hibernate creates or validates the `database_connections` table. Check with MySQL client.

**Phase 4 — Create `DatabaseConnectionRepository`**
- Create a JPA repository interface extending `JpaRepository<DatabaseConnection, Long>`.
- **Learn:** What `JpaRepository` provides, how repository proxies work, what happens during `save()` and `findById()`.
- **Prove it:** Write a `CommandLineRunner` that saves one `DatabaseConnection` record and reads it back by ID. Print non-sensitive fields.

**Phase 5 — Understand the two-database architecture**
- Create a `database` package.
- Create an empty `JdbcUrlBuilder` class (just the skeleton — no logic yet).
- **Learn:** Why JPA fits the known application schema; why dynamic `JdbcTemplate` is needed for unknown external schemas; why you should never make JPA entities for tables in user databases.
- **Prove it:** You can clearly explain the two-database architecture to someone else.

---

### Stage 2 — Dynamic Database Connection (Phases 6–10)

**Phase 6 — Build `JdbcUrlBuilder`**
- Accept a `DatabaseConnection` object.
- Produce: `jdbc:mysql://{host}:{port}/{databaseName}`
- Add basic validation (host not null, port 1–65535, db name not empty).
- **Learn:** Why connection details are stored as separate fields, why JDBC URLs are constructed internally.
- **Prove it:** Unit tests for different hosts, ports, and database names.

**Phase 7 — Manually create a second DataSource at runtime**
- Load one `DatabaseConnection` from the application DB (using your repository).
- Use its config to manually create a `DriverManagerDataSource`.
- Do NOT create a `JdbcTemplate` yet.
- **Learn:** The difference between Spring Boot's auto-configured DataSource and your manually created dynamic one. Why the second datasource doesn't exist at startup.
- **Prove it:** Call `dataSource.getConnection()` and print the connection object. Observe it's a runtime-created connection.

**Phase 8 — Test the dynamic connection validity**
- Call `dataSource.getConnection().isValid(5)` (5-second timeout).
- Print whether the connection is valid.
- Handle failure gracefully (wrong host, bad port, bad credentials).
- Close the connection in `finally` or try-with-resources.
- **Learn:** JDBC Connection lifecycle, resource management, connection timeouts, auth failures vs unreachable hosts.
- **Prove it:** Valid credentials → "Connection valid: true". Invalid credentials → graceful error, no crash.

**Phase 9 — Create `DynamicJdbcTemplateProvider`**
- Build a class that:
  1. Accepts a `DatabaseConnection`.
  2. Builds JDBC URL via `JdbcUrlBuilder`.
  3. Creates a `DriverManagerDataSource`.
  4. Wraps it in a `JdbcTemplate`.
  5. Returns the `JdbcTemplate`.
- **Learn:** What `JdbcTemplate` abstracts away. Why you're dynamically constructing it vs using `@Autowired JdbcTemplate`.
- **Prove it:** Given a `DatabaseConnection`, your provider returns a working `JdbcTemplate`.

**Phase 10 — Execute `SHOW TABLES` against the target database**
- Load a target DB configuration from the application DB.
- Build a dynamic `JdbcTemplate` via your provider.
- Execute `SHOW TABLES`.
- Return table names as `List<String>`.
- **Prove it:** You see actual tables from your external MySQL database printed in the console.

> **🎉 Milestone:** You've validated the project's most fundamental technical assumption:
> `Spring Boot → App DB → stored config → dynamic connection → external MySQL → SHOW TABLES`

---

### Stage 3 — Read & Write Query Execution (Phases 11–14)

**Phase 11 — Execute a hardcoded `SELECT` query**
- Run a known `SELECT` against your test target database.
- Map results to `List<Map<String, Object>>` using `queryForList()`.
- **Learn:** Dynamic result sets, why entity mapping doesn't work for unknown schemas.
- **Prove it:** Print the result rows. You queried a table without having a JPA entity for it.

**Phase 12 — Execute a hardcoded `UPDATE` query**
- Run one safe `UPDATE` against a test table (update a known record by ID).
- Return affected row count using `JdbcTemplate.update()`.
- **Learn:** `query()` vs `update()` difference, why affected-row counts matter, transaction implications.
- **Prove it:** Row count matches expectations. Verify the change in your MySQL client.

**Phase 13 — Add SELECT-only query validation**
- Before executing a query, validate it starts with `SELECT` (case-insensitive, trimmed).
- Reject `INSERT`, `UPDATE`, `DELETE`, `DROP`, `ALTER`, `CREATE`, `TRUNCATE`.
- Reject multiple statements (split by `;`).
- **Learn:** Why `trim().toUpperCase().startsWith("SELECT")` alone is insufficient — comments, whitespace, CTEs, bypass possibilities. Limitations of string-based SQL validation.
- **Prove it:** `DELETE FROM users` → rejected. `SELECT * FROM users` → works.

**Phase 14 — Add result-size limits and query timeouts**
- Configure: max rows (1000), query timeout (30s), connection timeout (10s), max query length (10000).
- When a SELECT result is truncated, include: `{ "returnedRows": 1000, "truncated": true }`.
- **Learn:** Why unlimited results are dangerous, memory exhaustion, server-side vs application-side limits. Slow query risks.
- **Prove it:** Query a table with >1000 rows → exactly 1000 rows with `truncated: true`.

---

### Stage 4 — Secure Credential Storage (Phases 15–17)

**Phase 15 — Build `DatabaseCredentialEncryptor`**
- Implement `encrypt(plainPassword)` and `decrypt(encryptedPassword)`.
- Use symmetric encryption (e.g., AES-256-GCM).
- Store the encryption key in `application.properties` (improve later).
- **Learn:** Encryption vs hashing. Why DB passwords must be encrypted (reversible). Why API keys should be hashed (one-way). Key management basics.
- **Prove it:** Unit test: encrypt a password → decrypt → original matches.

**Phase 16 — Encrypt before storing, decrypt before connecting**
- Rename DB column from `password` to `encrypted_password`.
- Before saving a `DatabaseConnection`, encrypt the password.
- When creating a dynamic connection, decrypt the password.
- Ensure decrypted credentials are **never logged**.
- **Prove it:** Query `database_connections` table directly — password column contains ciphertext. Dynamic connection still works.

**Phase 17 — Create `DatabaseConnectionService` with full CRUD**
- Implement: `create()`, `findById()`, `findAll()`, `update()`, `delete()` (or `deactivate()`).
- Encryption/decryption happens inside the service.
- No controller yet — pure service layer.
- **Learn:** Why business logic belongs in services, entity ownership boundaries, transaction basics.
- **Prove it:** Write a test that creates, reads, updates, and deactivates a `DatabaseConnection`.

---

### Stage 5 — REST APIs for Database Configuration (Phases 18–20)

**Phase 18 — Create request and response DTOs**
- `CreateDatabaseConnectionRequest`, `UpdateDatabaseConnectionRequest`.
- `DatabaseConnectionResponse` (all fields **except** `encryptedPassword`).
- `TestConnectionRequest`, `TestConnectionResponse`.
- **Learn:** Why returning JPA entities directly is dangerous, request/response boundaries, sensitive-field protection.
- **Prove it:** DTOs compile. No encrypted password leaks in responses.

**Phase 19 — Create `DatabaseConnectionController` (full CRUD + test)**
- `POST /database-connections` — create.
- `GET /database-connections` — list all.
- `GET /database-connections/{id}` — get one.
- `PUT /database-connections/{id}` — update.
- `DELETE /database-connections/{id}` — deactivate.
- `POST /database-connections/test` — test connection dynamically.
- **Prove it:** Use Postman or `curl` to manage target DB configurations end-to-end.

**Phase 20 — Add validation and global exception handling**
- Add `@NotBlank`, `@NotNull`, `@Min`/`@Max` for port, name constraints.
- `@Valid` on controller method parameters.
- Create `GlobalExceptionHandler` with `@RestControllerAdvice`.
- Map validation errors, connection errors, not-found errors to structured JSON.
- Never expose stack traces to clients.
- **Learn:** Bean Validation, `@Valid`, why invalid data is rejected at system boundaries, `@RestControllerAdvice`, exception translation.
- **Prove it:** Send invalid data (empty name, port 0) — get a clean JSON validation error.

---

### Stage 6 — API Keys (Phases 21–24)

**Phase 21 — Create `ApiKey` entity and repository**
- Fields: `id`, `databaseConnection` (ManyToOne), `name`, `keyPrefix`, `keyHash`, `isActive`, `expiresAt`, `createdAt`, `lastUsedAt`.
- `ApiKeyRepository` extending `JpaRepository`.
- **Learn:** `@ManyToOne`, foreign keys, entity relationships.
- **Prove it:** Save and read an `ApiKey` record with its associated `DatabaseConnection`.

**Phase 22 — Generate secure API keys**
- Generate keys like: `mcp_live_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx` (prefix + secure random).
- Use `SecureRandom` for the random portion.
- Return the complete raw key **only once** (when generated).
- Store only `keyPrefix` and `keyHash` in the database.
- **Learn:** Secure randomness, key prefixes, why secrets are shown only once.
- **Prove it:** Generate a key — raw key printed once. Database has only prefix and hash.

**Phase 23 — Hash API keys before storage**
- Hash the raw key (SHA-256 with salt or bcrypt) before saving.
- When validating, hash the provided key and compare against stored hash.
- **Learn:** Hashing vs encryption for different use cases, API-key lookup strategies, constant-time comparison.
- **Prove it:** Raw key never stored. Correct key validates. Incorrect key fails.

**Phase 24 — Validate API keys and resolve the `DatabaseConnection`**
- Create `ApiKeyValidationService`:
  1. Accept raw API key.
  2. Hash it.
  3. Find matching active, non-expired record.
  4. Return associated `DatabaseConnection`.
- **Prove it:** Valid key → `DatabaseConnection`. Invalid/expired key → error.

---

### Stage 7 — Permissions (Phases 25–27)

**Phase 25 — Create `ApiKeyPermission` entity and `DatabasePermission` enum**
- `DatabasePermission` enum: `SHOW_TABLES`, `DESCRIBE_TABLE`, `SELECT`, `EXPLAIN`.
- `ApiKeyPermission` entity: `id`, `apiKey` (ManyToOne), `permission`.
- `ApiKeyPermissionRepository`.
- **Learn:** Many-to-one relationships with join columns, enum mapping strategies.
- **Prove it:** Assign `SHOW_TABLES` and `SELECT` permissions to an API key. Verify in database.

**Phase 26 — Create `PermissionService`**
- Given API key ID + required permission → return `true`/`false`.
- **Prove it:** Key with `SELECT` returns `true` for `SELECT`, `false` for `DELETE`.

**Phase 27 — Enforce permissions before query execution**
- Before executing any database operation:
  1. Resolve API key → `DatabaseConnection`.
  2. Check required permission via `PermissionService`.
  3. If missing → reject with `PERMISSION_DENIED`.
- **Prove it:** Key with only `SHOW_TABLES` can call `SHOW TABLES` but not `SELECT`.

---

### Stage 8 — Audit Logging (Phases 28–30)

**Phase 28 — Create `AuditLog` entity and repository**
- Fields: `id`, `apiKey` (nullable FK), `databaseConnection` (nullable FK), `operation`, `requestPayload`, `responseSummary`, `success`, `rowsAffected`, `executionTimeMs`, `errorCode`, `errorMessage`, `createdAt`.
- **Prove it:** Save an audit log record manually and read it back.

**Phase 29 — Audit successful operations**
- After successful query execution, record: API key, database, operation, rows, execution time, response summary.
- **Prove it:** Execute `SELECT` — audit log entry with `success: true` appears.

**Phase 30 — Audit failed operations**
- Record failed operations with error code + message.
- Never log raw API keys, the configured single-user password, decrypted credentials, or auth headers.
- **Prove it:** Execute invalid query — audit log with `success: false` and error details appears.

---

### Stage 9 — MCP Integration (Phases 31–34)

**Phase 31 — Add MCP dependency and configure the server**
- Add Spring AI MCP server dependency to `pom.xml`.
- Configure MCP server transport (STDIO or SSE).
- **Learn:** What MCP is, MCP server vs client, tools, transport, how Spring AI integrates.
- **Prove it:** An MCP client can connect to your server (even with no useful tools yet).

**Phase 32 — Create a trivial `ping` MCP tool**
- `ping()` → returns `"pong"`.
- Purpose: isolate MCP learning from database complexity.
- **Prove it:** Call `ping` from MCP client, get `"pong"`.

**Phase 33 — Expose `show_tables` and `describe_table` as MCP tools**
- Wire your existing, already-tested execution service into MCP tools.
- Full flow: MCP Client → API key validation → permission check → resolve DB config → dynamic JdbcTemplate → target DB → audit → response.
- **Prove it:** Call `show_tables` and `describe_table` from MCP client. Get real results from your target database.

**Phase 34 — Expose `execute_select` and `explain_query` as MCP tools**
- Wire the already-secured SELECT execution engine and EXPLAIN functionality into MCP.
- **Prove it:** `SELECT * FROM users` and `EXPLAIN SELECT * FROM users` via MCP return correct results.

> **🎉 Milestone:** You have a working MCP server that securely connects to user databases, executes read queries, checks permissions, and audits everything.

---

### Stage 10 — Single-User Authentication (Phases 35–36)

**Phase 35 — Configure single-user credentials**
- Add `mysql-mcp.auth.username` and `mysql-mcp.auth.password` to `application.properties`, with sane defaults (e.g., `admin` / `admin123`).
- Create an `AppAuthProperties` (`@ConfigurationProperties`) class binding these values.
- Confirm the values can be overridden via environment variables (`MYSQL_MCP_AUTH_USERNAME`, `MYSQL_MCP_AUTH_PASSWORD`) using Spring Boot's relaxed binding — no custom code needed for the override itself.
- Log a startup warning if default credentials are still active.
- **Learn:** `@ConfigurationProperties`, Spring Boot relaxed environment-variable binding, why secrets shouldn't be hardcoded, why there's no `User` entity when there's only one user.
- **Prove it:** Application starts with default credentials and logs a warning. Setting the environment variables changes the effective login credentials without touching any file.

**Phase 36 — Login endpoint and JWT authentication**
- Implement `POST /auth/login`: accept username + password, compare against `AppAuthProperties` (constant-time comparison), return a JWT on success.
- Protect all management REST endpoints (`database-connections`, `api-keys`, `audit-logs`) with JWT authentication.
- No ownership checks are needed anywhere — every resource in the application database implicitly belongs to the single operator.
- **Prove it:** Correct configured credentials → JWT. Wrong credentials → `INVALID_CREDENTIALS`. Protected endpoints reject requests without a valid JWT.

---

### Stage 11 — Mutation Operations (Phases 37–39)

**Phase 37 — Implement `execute_insert` as an MCP tool**
- Add `INSERT` to permissions (already in the enum).
- Build `executeInsert()`: validate INSERT, `JdbcTemplate.update()`, return affected row count, audit.
- **Prove it:** Insert a row via MCP. Verify in target database.

**Phase 38 — Implement `execute_update` and `execute_delete` as MCP tools**
- Add `UPDATE` and `DELETE` permissions.
- Build `executeUpdate()` and `executeDelete()`.
- **Safety:** Reject mutations without `WHERE` clause:
  - `UPDATE users SET active = false` → rejected.
  - `UPDATE users SET active = false WHERE id = 1` → allowed.
  - `DELETE FROM users` → rejected.
  - `DELETE FROM users WHERE id = 1` → allowed.
- Audit each operation.
- **Prove it:** WHERE-less DELETE rejected. Safe DELETE works.

**Phase 39 — Implement DDL operations as MCP tools**
- Add `CREATE_TABLE`, `ALTER_TABLE`, `DROP_TABLE` permissions.
- Build `executeDdl()` in `DatabaseExecutionService`.
- **Safeguards:** Explicit DDL permission required (separate from mutation). Full DDL logged in audit. Consider confirmation step for `DROP TABLE`.
- **Prove it:** Create a table via MCP, alter it, drop it. Each operation audited.

---

### Future Phases (Not Yet Scheduled)
- **Dynamic Connection Pooling** — HikariCP pools per database configuration.
- **Frontend** — Web UI consuming REST APIs.
- **Rate Limiting** — Per API key rate limits.
- **Key Rotation** — Rotate database credentials without downtime.

---

## 11. Definition of MVP

The project is considered a functional MVP when:

- [x] The operator can log in using configured (or default) single-user credentials.
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

Once this works, the most fundamental technical assumption behind the project is validated. Everything else — API keys, permissions, MCP tools, audit logging, single-user login, and UI — builds on that foundation.