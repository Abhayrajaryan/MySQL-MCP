package com.mysqlmcp.enums;

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
    DROP_TABLE;

    /**
     * DML operations that mutate row data. Gated behind
     * {@code mysql-mcp.security.enable-write-operations} (disabled by default).
     */
    public boolean isWriteOperation() {
        return this == INSERT || this == UPDATE || this == DELETE;
    }

    /**
     * DDL operations that mutate schema. Gated behind
     * {@code mysql-mcp.security.enable-ddl-operations} (disabled by default).
     */
    public boolean isDdlOperation() {
        return this == CREATE_TABLE || this == ALTER_TABLE || this == DROP_TABLE;
    }
}