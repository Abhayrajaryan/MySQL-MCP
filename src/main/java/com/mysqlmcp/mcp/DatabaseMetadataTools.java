package com.mysqlmcp.mcp;

import com.mysqlmcp.database.DatabaseCredentialEncryptor;
import com.mysqlmcp.dto.DatabaseAccessContext;
import com.mysqlmcp.dto.DatabaseCredentials;
import com.mysqlmcp.enums.DatabasePermission;
import com.mysqlmcp.exception.PermissionDeniedException;
import com.mysqlmcp.service.ApiKeyValidationService;
import com.mysqlmcp.service.AuditLogService;
import com.mysqlmcp.service.DatabaseExecutionService;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMetadataTools {

    private final ApiKeyValidationService apiKeyValidationService;
    private final DatabaseExecutionService executionService;
    private final DatabaseCredentialEncryptor encryptor;
    private final AuditLogService auditLogService;

    public DatabaseMetadataTools(ApiKeyValidationService apiKeyValidationService,
                                 DatabaseExecutionService executionService,
                                 DatabaseCredentialEncryptor encryptor,
                                 AuditLogService auditLogService) {
        this.apiKeyValidationService = apiKeyValidationService;
        this.executionService = executionService;
        this.encryptor = encryptor;
        this.auditLogService = auditLogService;
    }

    public DatabaseExecutionService.ShowTablesResult showTables(DatabaseAccessContext context) {
        apiKeyValidationService.checkPermission(context, DatabasePermission.SHOW_TABLES);
        DatabaseCredentials credentials = buildCredentials(context);
        return executionService.showTables(credentials);
    }

    public DatabaseExecutionService.DescribeTableResult describeTable(DatabaseAccessContext context, String tableName) {
        apiKeyValidationService.checkPermission(context, DatabasePermission.DESCRIBE_TABLE);
        DatabaseCredentials credentials = buildCredentials(context);
        return executionService.describeTable(credentials, tableName);
    }

    private DatabaseCredentials buildCredentials(DatabaseAccessContext context) {
        String decryptedPassword = encryptor.decrypt(context.encryptedPassword());
        return new DatabaseCredentials(
                context.host(),
                context.port(),
                context.databaseName(),
                context.dbUsername(),
                decryptedPassword
        );
    }
}