# MySQL MCP Server - Setup and Configuration Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Database Setup](#database-setup)
3. [Configuration](#configuration)
4. [Running the Application](#running-the-application)
5. [Using the Web UI](#using-the-web-ui)
6. [Using MCP Tools](#using-mcp-tools)
7. [Security Considerations](#security-considerations)

---

## Prerequisites

- **Java 21** or higher
- **MySQL 8.0** or higher
- **Maven 3.8+** (or use the included Maven wrapper)
- **2GB RAM** minimum (4GB recommended)

---

## Database Setup

### 1. Create Application Database

The application needs a MySQL database to store its configuration (database connections, API keys, permissions, audit logs).

```sql
CREATE DATABASE mysql_mcp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Create a MySQL User (Recommended)

For security, create a dedicated MySQL user instead of using root:

```sql
CREATE USER 'mysql_mcp'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON mysql_mcp.* TO 'mysql_mcp'@'localhost';
FLUSH PRIVILEGES;
```

---

## Configuration

### Option 1: Using application.properties (Default)

Edit `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8081

# Application Database Connection
spring.datasource.url=jdbc:mysql://localhost:3306/mysql_mcp?serverTimezone=Asia/Kolkata
spring.datasource.username=root
spring.datasource.password=your_password

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.open-in-view=false

# Encryption Key (Generate your own!)
# Generate with: openssl rand -base64 32
mysql-mcp.encryption.key=your_generated_key_here

# Single-User Authentication (CHANGE THESE!)
mysql-mcp.auth.username=admin
mysql-mcp.auth.password=your_secure_password_here

# JWT Secret (Generate your own!)
# Generate with: openssl rand -base64 32
mysql-mcp.jwt.secret=your_jwt_secret_here

# MCP Server Configuration
spring.ai.mcp.server.transport=sse
spring.ai.mcp.server.sse.host=localhost
spring.ai.mcp.server.sse.port=8081
spring.ai.mcp.server.sse.path=/api/mcp
```

### Option 2: Using Environment Variables (Recommended for Production)

Override any property using environment variables:

```bash
# Database configuration
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/mysql_mcp?serverTimezone=Asia/Kolkata
export SPRING_DATASOURCE_USERNAME=mysql_mcp
export SPRING_DATASOURCE_PASSWORD=your_secure_password

# Server port
export SPRING_SERVER_PORT=8081

# Authentication
export MYSQL_MCP_AUTH_USERNAME=admin
export MYSQL_MCP_AUTH_PASSWORD=your_secure_password

# Encryption key
export MYSQL_MCP_ENCRYPTION_KEY=your_generated_key

# JWT secret
export MYSQL_MCP_JWT_SECRET=your_jwt_secret
```

### Option 3: Using Command Line Arguments

```bash
java -jar mysql-mcp-server.jar \
  --spring.datasource.url=jdbc:mysql://localhost:3306/mysql_mcp \
  --spring.datasource.username=mysql_mcp \
  --spring.datasource.password=your_password \
  --server.port=8081 \
  --mysql-mcp.auth.username=admin \
  --mysql-mcp.auth.password=your_secure_password
```

### Option 4: External Configuration File

Create an external `application.properties` file and run:

```bash
java -jar mysql-mcp-server.jar \
  --spring.config.location=file:/path/to/external/application.properties
```

---

## Running the Application

### Build the Application

```bash
mvn clean package -DskipTests
```

### Run the Application

```bash
java -jar target/mysql-mcp-server-0.0.1-SNAPSHOT.jar
```

### Run with Custom Configuration

```bash
java -jar target/mysql-mcp-server-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:mysql://localhost:3306/mysql_mcp \
  --spring.datasource.username=root \
  --spring.datasource.password=your_password \
  --server.port=8081
```

---

## Using the Web UI

### 1. Access the Application

Open your browser and navigate to:
```
http://localhost:8081/
```

### 2. Login

- **Default username:** `admin`
- **Default password:** `admin123` (from application.properties)

**⚠️ IMPORTANT:** Change the default credentials in production!

### 3. Configure Database Connections

After logging in, you'll see the dashboard. Click **"+ New Connection"** to add a target database:

- **Connection Name:** Friendly name (e.g., "Production DB")
- **Host:** Database host (e.g., localhost)
- **Port:** Database port (default: 3306)
- **Database Name:** Target database name
- **Username:** Database username
- **Password:** Database password (will be encrypted)

### 4. Create API Keys

For each database connection, create API keys:

1. Click **"Create API Key"** on the connection card
2. Enter a name (e.g., "Read-only key")
3. Select permissions:
   - `SHOW_TABLES` - List tables
   - `DESCRIBE_TABLE` - Get table structure
   - `SELECT` - Execute SELECT queries
   - `EXPLAIN` - Analyze query performance
   - `INSERT` - Insert data
   - `UPDATE` - Update data
   - `DELETE` - Delete data
   - `CREATE_TABLE` - Create tables
   - `ALTER_TABLE` - Alter tables
   - `DROP_TABLE` - Drop tables
4. Click **"Generate API Key"**
5. **⚠️ SAVE THE KEY IMMEDIATELY** - It will only be shown once!

---

## Using MCP Tools

The MCP server is automatically configured and discovers all tools with `@Tool` annotations. AI assistants can connect to the MCP server and call these tools directly.

**MCP Server Endpoint:** `http://localhost:8081`

### Available MCP Tools

#### 1. showTables
List all tables in the database.

**Required Permission:** `SHOW_TABLES`

**Description:** List all tables in the database.

**Required Permission:** `SHOW_TABLES`

**Parameters:**
- `apiKey` (string): API key for authentication

**Returns:** CSV formatted list of table names

**Example Response:**
```
Tables_in_mydb
users
products
orders
success,count,3
```

#### 2. describeTable
Get the structure of a table.

**Required Permission:** `DESCRIBE_TABLE`

**Description:** Get the structure of a table.

**Required Permission:** `DESCRIBE_TABLE`

**Parameters:**
- `apiKey` (string): API key for authentication
- `tableName` (string): Name of the table to describe

**Returns:** CSV formatted table structure

**Example Response:**
```
Field,Type,Null,Key,Default,Extra
id,bigint,NO,PRI,NULL,auto_increment
name,varchar(255),YES,NULL,NULL,
email,varchar(255),YES,UNI,NULL,
success,count,3
```

#### 3. executeSelect
Execute a SELECT query.

**Required Permission:** `SELECT`

**Description:** Execute a SELECT query on the database.

**Required Permission:** `SELECT`

**Parameters:**
- `apiKey` (string): API key for authentication
- `query` (string): SELECT query to execute

**Returns:** CSV formatted query results

**Example Response:**
```
id,name,email
1,Alice,alice@example.com
2,Bob,bob@example.com
success,count,2
```

#### 4. explainQuery
Analyze query performance.

**Required Permission:** `EXPLAIN`

**Description:** Analyze query performance using EXPLAIN.

**Required Permission:** `EXPLAIN`

**Parameters:**
- `apiKey` (string): API key for authentication
- `query` (string): Query to explain

**Returns:** CSV formatted EXPLAIN results

**Example Response:**
```
id,select_type,table,type,possible_keys,key,key_len,ref,rows,Extra
1,SIMPLE,users,const,PRIMARY,PRIMARY,8,const,1,
success,count,1
```

#### 5. executeInsert
Insert data into a table.

**Required Permission:** `INSERT`

**Description:** Execute an INSERT query.

**Required Permission:** `INSERT`

**Parameters:**
- `apiKey` (string): API key for authentication
- `query` (string): INSERT query to execute

**Returns:** CSV with affected rows count

**Example Response:**
```
success,affectedRows,1
```

#### 6. executeUpdate
Update data in a table.

**Required Permission:** `UPDATE`

**Description:** Execute an UPDATE query.

**Required Permission:** `UPDATE`

**Parameters:**
- `apiKey` (string): API key for authentication
- `query` (string): UPDATE query to execute

**Returns:** CSV with affected rows count

**Example Response:**
```
success,affectedRows,1
```

#### 7. executeDelete
Delete data from a table.

**Required Permission:** `DELETE`

**Description:** Execute a DELETE query.

**Required Permission:** `DELETE`

**Parameters:**
- `apiKey` (string): API key for authentication
- `query` (string): DELETE query to execute

**Returns:** CSV with affected rows count

**Example Response:**
```
success,affectedRows,1
```

#### 8. createTable
Create a new table.

**Required Permission:** `CREATE_TABLE`

**Description:** Execute a CREATE TABLE query.

**Required Permission:** `CREATE_TABLE`

**Parameters:**
- `apiKey` (string): API key for authentication
- `query` (string): CREATE TABLE query to execute

**Returns:** CSV with success message

**Example Response:**
```
success,message,Table created successfully
```

#### 9. alterTable
Modify an existing table.

**Required Permission:** `ALTER_TABLE`

**Description:** Execute an ALTER TABLE query.

**Required Permission:** `ALTER_TABLE`

**Parameters:**
- `apiKey` (string): API key for authentication
- `query` (string): ALTER TABLE query to execute

**Returns:** CSV with success message

**Example Response:**
```
success,message,Table altered successfully
```

#### 10. dropTable
Drop a table.

**Required Permission:** `DROP_TABLE`

**Description:** Execute a DROP TABLE query.

**Required Permission:** `DROP_TABLE`

**Parameters:**
- `apiKey` (string): API key for authentication
- `query` (string): DROP TABLE query to execute

**Returns:** CSV with success message

**Example Response:**
```
success,message,Table dropped successfully
```

### Error Responses

All errors are returned in CSV format:

```csv
error,code,INVALID_CREDENTIALS
error,message,Invalid API key
```

**Common Error Codes:**
- `INVALID_CREDENTIALS` - Invalid API key
- `API_KEY_DISABLED` - API key is disabled
- `PERMISSION_DENIED` - Missing required permission
- `INVALID_QUERY` - Invalid SQL query
- `DATABASE_UNREACHABLE` - Cannot connect to database

---

## Security Considerations

### 1. Change Default Credentials

**⚠️ CRITICAL:** Always change the default admin credentials in production:

```properties
mysql-mcp.auth.username=your_secure_username
mysql-mcp.auth.password=your_strong_password_here
```

### 2. Generate Strong Encryption Keys

Generate secure random keys for encryption:

```bash
# Generate encryption key
openssl rand -base64 32

# Generate JWT secret
openssl rand -base64 32
```

### 3. Use Environment Variables in Production

Never commit sensitive credentials to version control. Use environment variables:

```bash
export MYSQL_MCP_AUTH_USERNAME=admin
export MYSQL_MCP_AUTH_PASSWORD=your_secure_password
export MYSQL_MCP_ENCRYPTION_KEY=your_key
export MYSQL_MCP_JWT_SECRET=your_jwt_secret
```

### 4. Use Dedicated Database User

Don't use root. Create a dedicated MySQL user with limited privileges.

### 5. Enable HTTPS in Production

For production deployments, use a reverse proxy (Nginx/Apache) with SSL/TLS.

### 6. Restrict API Key Permissions

Follow the principle of least privilege:
- Create separate API keys for different operations
- Grant only the minimum required permissions
- Regularly rotate API keys

### 7. Monitor Audit Logs

The application logs all operations. Regularly review audit logs for suspicious activity.

---

## Troubleshooting

### Application won't start

1. Check MySQL is running: `mysql -u root -p`
2. Verify database exists: `SHOW DATABASES;`
3. Check application.properties credentials
4. Review logs for error messages

### Can't connect to target database

1. Verify target database is running
2. Check firewall rules
3. Verify username/password in database connection
4. Test connection from MySQL client first

### MCP tools not working

1. Verify MCP server is running: Check logs for "MCP Server started"
2. Check API key has required permissions
3. Verify API key is active
4. Check network connectivity to SSE endpoint

### Permission denied errors

1. Ensure API key has the required permission
2. Check permission was assigned when creating the API key
3. Verify the permission enum matches the operation

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    MySQL MCP Server                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Web UI (Thymeleaf)                                   │  │
│  │  - Login Page                                         │  │
│  │  - Dashboard (Database connections, API keys)         │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  REST API Layer                                       │  │
│  │  - /api/auth/login                                    │  │
│  │  - /api/auth/logout                                   │  │
│  │  - /api/database-connections                          │  │
│  │  - /api/api-keys                                      │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  MCP Server (SSE Transport)                           │  │
│  │  - /api/mcp                                           │  │
│  │  - Tools: showTables, executeSelect, etc.             │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Application Database (MySQL)                         │  │
│  │  - database_connections                               │  │
│  │  - api_keys                                           │  │
│  │  - api_key_permissions                                │  │
│  │  - audit_logs                                         │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ Dynamic Connection
                              ▼
                    ┌───────────────────┐
                    │  Target Database  │
                    │  (User's MySQL)   │
                    └───────────────────┘
```

---

## Next Steps

1. ✅ Build and run the application
2. ✅ Login with default credentials
3. ✅ Change default credentials
4. ✅ Add your first database connection
5. ✅ Create API keys with appropriate permissions
6. ✅ Test MCP tools with your AI assistant
7. ✅ Monitor audit logs
8. ✅ Set up monitoring and backups

---

## Support

For issues and questions:
- Check the logs in `logs/` directory
- Review audit logs in the dashboard
- Consult the PROJECT_OVERVIEW.md for architecture details