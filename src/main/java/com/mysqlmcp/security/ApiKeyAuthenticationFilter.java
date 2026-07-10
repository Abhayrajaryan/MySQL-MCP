package com.mysqlmcp.security;

import com.mysqlmcp.service.ApiKeyValidationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyValidationService apiKeyValidationService;

    public ApiKeyAuthenticationFilter(ApiKeyValidationService apiKeyValidationService) {
        this.apiKeyValidationService = apiKeyValidationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String rawApiKey = authHeader.substring(7);

            try {
                var context = apiKeyValidationService.validateAndResolve(rawApiKey);
                ApiKeyAuthentication authentication = new ApiKeyAuthentication(context);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // Authentication failed - will be handled by SecurityConfig
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}