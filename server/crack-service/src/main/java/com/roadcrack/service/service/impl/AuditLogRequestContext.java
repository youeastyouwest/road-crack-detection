package com.roadcrack.service.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

final class AuditLogRequestContext {

    private AuditLogRequestContext() {
    }

    static ResolvedContext resolve() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return new ResolvedContext(null, null, null);
        }

        HttpServletRequest request = servletRequestAttributes.getRequest();
        Long userId = castLong(request.getAttribute("userId"));
        String username = castString(request.getAttribute("username"));
        return new ResolvedContext(userId, username, resolveClientIp(request));
    }

    private static Long castLong(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        return null;
    }

    private static String castString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (hasText(forwardedFor) && !"unknown".equalsIgnoreCase(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (hasText(realIp) && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    record ResolvedContext(Long userId, String username, String ip) {
    }
}
