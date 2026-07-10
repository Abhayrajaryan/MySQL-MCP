package com.mysqlmcp.security;

import com.mysqlmcp.dto.DatabaseAccessContext;
import org.springframework.security.authentication.AbstractAuthenticationToken;

public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final String rawApiKey;
    private final DatabaseAccessContext context;

    public ApiKeyAuthentication(String rawApiKey) {
        super(null);
        this.rawApiKey = rawApiKey;
        this.context = null;
        setAuthenticated(false);
    }

    public ApiKeyAuthentication(DatabaseAccessContext context) {
        super(null);
        this.rawApiKey = null;
        this.context = context;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return rawApiKey;
    }

    @Override
    public Object getPrincipal() {
        return context;
    }

    public DatabaseAccessContext getContext() {
        return context;
    }
}