package com.mysqlmcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mysql-mcp")
public class MysqlMcpProperties {

    private final Execution execution = new Execution();
    private final Connection connection = new Connection();

    public Execution getExecution() {
        return execution;
    }

    public Connection getConnection() {
        return connection;
    }

    public static class Execution {
        private int maxRows = 1000;
        private int queryTimeoutSeconds = 30;
        private int maxQueryLength = 10000;

        public int getMaxRows() { return maxRows; }
        public void setMaxRows(int maxRows) { this.maxRows = maxRows; }
        public int getQueryTimeoutSeconds() { return queryTimeoutSeconds; }
        public void setQueryTimeoutSeconds(int queryTimeoutSeconds) { this.queryTimeoutSeconds = queryTimeoutSeconds; }
        public int getMaxQueryLength() { return maxQueryLength; }
        public void setMaxQueryLength(int maxQueryLength) { this.maxQueryLength = maxQueryLength; }
    }

    public static class Connection {
        private int timeoutSeconds = 10;

        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }
}