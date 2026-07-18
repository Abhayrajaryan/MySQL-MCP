package com.mysqlmcp.mcp;

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
