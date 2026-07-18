package com.mysqlmcp.config;

import com.mysqlmcp.mcp.DatabaseMetadataTools;
import com.mysqlmcp.mcp.DatabaseModificationTools;
import com.mysqlmcp.mcp.DatabaseQueryTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider mcpTools(DatabaseQueryTools queryTools,
                                         DatabaseModificationTools modificationTools,
                                         DatabaseMetadataTools metadataTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(queryTools, modificationTools, metadataTools)
                .build();
    }
}
