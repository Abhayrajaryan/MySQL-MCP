package com.mysqlmcp.dto.request;

import lombok.Data;

@Data
public class UpsertDatabaseConnectionRequest {
    private Long id;               // null → create, present → update
    private String name;
    private String host;
    private Integer port;
    private String databaseName;
    private String dbUsername;
    private String password;       // plaintext, will be encrypted before storing

    // Permission flags
    private boolean showTables;
    private boolean describeTable;
    private boolean select;
    private boolean explain;
    private boolean insert;
    private boolean update;
    private boolean delete;
    private boolean createTable;
    private boolean alterTable;
    private boolean dropTable;
}
