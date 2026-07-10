package com.mysqlmcp.mcp;

import com.mysqlmcp.database.DatabaseCredentialEncryptor;
import com.mysqlmcp.dto.DatabaseAccessContext;
import com.mysqlmcp.dto.DatabaseCredentials;
import com.mysqlmcp.enums.DatabasePermission;
import com.mysqlmcp.service.ApiKeyValidationService;
import com.mysqlmcp.service.AuditLogService;
import com.mysqlmcp.service.DatabaseExecutionService;
import org.springframework.stereotype.Component;

@Component
public class DatabaseQueryTools {

    private final ApiKeyValidationService apiKeyValidationService;
    private final DatabaseExecutionService executionService;
    private final DatabaseCredentialEncryptor encryptor;
    private final AuditLogService auditLogService;

    public DatabaseQueryTools(ApiKeyValidationService apiKeyValidationService,
                              DatabaseExecutionService executionService,
                              DatabaseCredentialEncryptor encryptor,
                              AuditLogService auditLogService) {
        this.apiKeyValidationService = apiKeyValidationService;
        this.executionService = executionService;
        this.encryptor = encryptor;
        this.auditLogService = auditLogService;
    }

    public DatabaseExecutionService.SelectResult executeSelect(DatabaseAccessContext context, String query) {
        apiKeyValidationService.checkPermission(context, DatabasePermission.SELECT);
        DatabaseCredentials credentials = buildCredentials(context);
        return executionService.executeSelect(credentials, query);
    }

    public DatabaseExecutionService.ExplainResult explainQuery(DatabaseAccessContext context, String query) {
        apiKeyValidationService.checkPermission(context, DatabasePermission.EXPLAIN);
        DatabaseCredentials credentials = buildCredentials(context);
        return executionService.explainQuery(credentials, query);
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