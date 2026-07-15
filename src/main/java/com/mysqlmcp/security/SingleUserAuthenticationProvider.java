package com.mysqlmcp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SingleUserAuthenticationProvider implements AuthenticationProvider {

    @Value("${mysql-mcp.auth.username}")
    private String configuredUsername;

    @Value("${mysql-mcp.auth.password}")
    private String configuredPassword;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        if (configuredUsername.equals(username) && configuredPassword.equals(password)) {
            return new UsernamePasswordAuthenticationToken(username, null, List.of());
        }

        throw new BadCredentialsException("Invalid username or password");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}