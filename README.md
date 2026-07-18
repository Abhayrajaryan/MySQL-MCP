# MySQL MCP Server

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.7-green)
![MCP](https://img.shields.io/badge/MCP-Compatible-orange)

A self-hosted MySQL MCP (Model Context Protocol) server that allows AI assistants such as Claude Desktop, Cursor, and other MCP-compatible clients to securely access MySQL databases without exposing database credentials directly.

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- MySQL Server running with a user that can create databases

### Option 1: Download a Release

Download the latest `mysql-mcp-server-0.0.1-SNAPSHOT.jar` from the [Releases page](https://github.com/Abhayrajaryan/MySQL-MCP/releases).

Create a directory for the server, place the JAR inside, and create an `application.properties` file alongside it:

```properties
# Server port
server.port=8081

# Application database (where connections, keys, audit logs are stored)
spring.datasource.url=jdbc:mysql://localhost:3306/mysql_mcp
spring.datasource.username=root
spring.datasource.password=your_password

# Required: AES-256-GCM key for encrypting target database passwords (32 bytes, Base64-encoded)
# Generate one: openssl rand -base64 32
mysql-mcp.encryption.key=RkR9jz4v7r0lN2rWmL6w2S7rV0y7DkJ6jQ2m3dF1o8Q=

# Required: JWT secret (minimum 32 characters)
mysql-mcp.jwt.secret=your_jwt_secret_min_32_bytes

# Admin credentials
mysql-mcp.auth.username=admin
mysql-mcp.auth.password=admin123
```

Create the application database in MySQL:

```sql
CREATE DATABASE IF NOT EXISTS mysql_mcp;
```

Run the JAR:

```bash
java -jar mysql-mcp-server-0.0.1-SNAPSHOT.jar --spring.config.location=./application.properties
```

### Option 2: Clone and Build from Source

```bash
git clone https://github.com/Abhayrajaryan/MySQL-MCP.git
cd MySQL-MCP
```

Edit `src/main/resources/application.properties` with your own configuration, then build and run:

```bash
mvn clean package -DskipTests
java -jar target/mysql-mcp-server-0.0.1-SNAPSHOT.jar
```

Or run directly with Maven:

```bash
mvn spring-boot:run
```

### Override Configuration at Runtime

Any property can be overridden on the command line:

```bash
java -jar target/mysql-mcp-server-0.0.1-SNAPSHOT.jar \
  --server.port=8080 \
  --spring.datasource.password=another_password
```

### Verify It's Running

Open your browser at `http://localhost:8081/login` and log in with:
- Username: `admin`
- Password: `admin123`

If you see the dashboard, the server is running.

## Project Structure

```
src/main/java/com/mysqlmcp/
├── config/              # Configuration classes
├── controller/          # REST controllers
├── database/            # JDBC and encryption utilities
├── dto/                 # Request/Response DTOs
├── entity/              # JPA entities (ApiKey, DatabaseConnection, AuditLog, etc.)
├── enums/               # Permission enums
├── exception/           # Exception classes and global handler
├── mcp/                 # MCP tool definitions
├── repository/          # JPA repositories
├── security/            # JWT filter and security config
├── service/             # Business logic
└── util/                # Utilities (CsvUtils)
```

## How It Works

```
Admin ──► REST API ──► MySQL MCP Server ──► MCP Tools ──► Target MySQL Database
                                ▲
                                │
                          MCP Client (Claude Desktop, Cursor, etc.)
```

1. **Admin registers a MySQL database** via the dashboard or REST API
2. **Admin creates an API key** and assigns permissions
3. **MCP client connects** at `http://localhost:8081/sse`
4. **AI assistant calls MCP tools** — passes the API key as a parameter
5. **Server authenticates, authorises, runs the query** against the target database and returns results as CSV
6. **Every operation is logged** in the audit trail

## Configuration Reference

Application properties are defined in `application.properties`.

| Property | Description | Default |
|----------|-------------|---------|
| `server.port` | HTTP port | `8081` |
| `spring.datasource.url` | JDBC URL for the application database | `jdbc:mysql://localhost:3306/mysql_mcp` |
| `spring.datasource.username` | Application DB username | `root` |
| `spring.datasource.password` | Application DB password | *required* |
| `spring.jpa.hibernate.ddl-auto` | Schema generation strategy | `update` |
| `spring.jpa.show-sql` | Log Hibernate SQL | `true` |
| `mysql-mcp.auth.username` | Dashboard admin username | `admin` |
| `mysql-mcp.auth.password` | Dashboard admin password | `admin123` |
| `mysql-mcp.jwt.secret` | JWT signing secret (min 32 bytes) | *required* |
| `mysql-mcp.encryption.key` | AES-256-GCM key for target DB passwords (32 bytes Base64) | *required* |
| `mysql-mcp.security.enable-write-operations` | Allow INSERT/UPDATE/DELETE | `true` |
| `mysql-mcp.security.enable-ddl-operations` | Allow CREATE/ALTER/DROP TABLE | `false` |
| `mysql-mcp.query.timeout-seconds` | Query timeout in seconds | `10` |
| `mysql-mcp.query.max-rows` | Max rows returned per query | `1000` |
| `mysql-mcp.query.max-length` | Max query string length (characters) | `10000` |

## MCP Client Configuration

The server uses streamable HTTP transport at `/sse`. The API key is passed directly as a **parameter** to each MCP tool — not as an HTTP header.

### Claude Desktop

```json
{
  "mcpServers": {
    "mysql": {
      "url": "http://localhost:8081/sse"
    }
  }
}
```

### Cursor

```json
{
  "mcpServers": {
    "mysql": {
      "url": "http://localhost:8081/sse"
    }
  }
}
```

### Using the API Key

When an AI assistant calls an MCP tool, just tell it the API key value. The key is a regular function parameter, not a Bearer token. For example:

> "Use API key `mcp_live_M5bOcF4kIP5...` to query the database."

## Permissions

| Permission | Type | Description |
|------------|------|-------------|
| `SHOW_TABLES` | Read | List tables |
| `DESCRIBE_TABLE` | Read | Show table structure |
| `SELECT` | Read | Execute SELECT queries |
| `EXPLAIN` | Read | Execute EXPLAIN queries |
| `INSERT` | Write | Execute INSERT queries |
| `UPDATE` | Write | Execute UPDATE queries |
| `DELETE` | Write | Execute DELETE queries |
| `CREATE_TABLE` | DDL | Execute CREATE TABLE |
| `ALTER_TABLE` | DDL | Execute ALTER TABLE |
| `DROP_TABLE` | DDL | Execute DROP TABLE |

Write and DDL operations are disabled by default. Enable them via `mysql-mcp.security.enable-write-operations=true` and/or `mysql-mcp.security.enable-ddl-operations=true`.

## Audit Logging

Every query is logged with:
- API key name
- Target database connection
- Operation (tool/permission name)
- SQL query text
- Success/failure status
- Execution time in milliseconds
- Error message (if failed)
- Timestamp

Access logs via the dashboard Audit Log tab or `GET /api/audit-logs`.

## API Endpoints

All management endpoints require a JWT token from `POST /api/auth/login`.

### Auth

```
POST /api/auth/login
  Body: { "username": "admin", "password": "admin123" }
  Returns: { "data": { "accessToken": "..." } }

POST /api/auth/logout
  Header: Authorization: Bearer <token>
```

### Database Connections

```
GET  /api/database-connections         # List all
GET  /api/database-connections/{id}    # Get by ID
POST /api/database-connections         # Create or update
  Body: { "name": "...", "host": "...", "port": 3306,
          "databaseName": "...", "dbUsername": "...", "password": "..." }
```

### API Keys

```
POST /api/api-keys/connections/{connectionId}
  Body: { "name": "...", "permissions": ["SELECT", "SHOW_TABLES"] }

GET  /api/api-keys/connections/{connectionId}
```

### Audit Logs

```
GET  /api/audit-logs?connectionId=&operation=&from=&to=&page=0&size=25
GET  /api/audit-logs/summary
GET  /api/audit-logs/filter-options
```

### Config

```
GET  /api/config/security-defaults
```

## Current Limitations

- No SQL AST validation — queries are executed as-is against the target database
- Permissions are operation-based — no table-level or row-level security
- Single admin user for the management dashboard
- No rate limiting
- MySQL only (other databases not supported yet)
- Intended for development, internal tooling, and experimentation. Do not expose directly to the public Internet.

## Build Commands

```bash
mvn clean test          # Run tests
mvn clean package       # Build JAR (output in target/)
mvn spring-boot:run     # Run in development
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes and test
4. Open a pull request

## Acknowledgements

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [MySQL](https://www.mysql.com/)
- [Project Lombok](https://projectlombok.org/)
- [Spring AI](https://spring.io/projects/spring-ai)