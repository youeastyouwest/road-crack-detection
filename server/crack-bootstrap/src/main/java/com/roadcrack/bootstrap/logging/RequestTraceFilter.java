package com.roadcrack.bootstrap.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestTraceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = buildRequestUri(request);
        String clientIp = resolveClientIp(request);

        log.info("HTTP request started: method={}, uri={}, clientIp={}", method, uri, clientIp);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("HTTP request completed: method={}, uri={}, status={}, durationMs={}",
                    method,
                    uri,
                    response.getStatus(),
                    duration);
        }
    }

    private String buildRequestUri(HttpServletRequest request) {
        String queryString = request.getQueryString();
        return queryString == null || queryString.isBlank()
                ? request.getRequestURI()
                : request.getRequestURI() + "?" + queryString;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank() && !"unknown".equalsIgnoreCase(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
