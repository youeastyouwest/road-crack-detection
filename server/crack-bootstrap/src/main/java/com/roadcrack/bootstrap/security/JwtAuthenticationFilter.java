package com.roadcrack.bootstrap.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.service.security.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> WHITE_LIST = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/send-code",
            "/api/auth/refresh",
            "/api/auth/reset-password",
            "/api/role/list",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/webjars/**",
            "/favicon.ico",
            "/error",
            "/actuator/**"
    );

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtUtils jwtUtils, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        if (isWhiteListed(requestUri) || !requestUri.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            sendUnauthorized(response, "missing token");
            return;
        }
        if (!jwtUtils.validateToken(token)) {
            sendUnauthorized(response, "invalid token");
            return;
        }

        request.setAttribute("userId", jwtUtils.getUserId(token));
        request.setAttribute("username", jwtUtils.getUsername(token));
        filterChain.doFilter(request, response);
    }

    private boolean isWhiteListed(String uri) {
        return WHITE_LIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        String token = request.getHeader("Token");
        return StringUtils.hasText(token) ? token : null;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.failure(ResultCode.UNAUTHORIZED, message)));
    }
}
