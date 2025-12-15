package com.crudzaso.cityhelp.auth.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.crudzaso.cityhelp.auth.infrastructure.dto.AuthResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT authentication entry point for unauthorized access.
 * Returns JSON response with Spanish error messages.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        AuthResponse errorResponse = AuthResponse.error("No autorizado - Token inválido o expirado");

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}