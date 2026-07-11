package com.roadcrack.service.service.impl;

import com.roadcrack.api.request.auth.ChangePasswordRequest;
import com.roadcrack.api.request.auth.LoginRequest;
import com.roadcrack.api.request.auth.RegisterRequest;
import com.roadcrack.api.request.auth.ResetPasswordRequest;
import com.roadcrack.api.response.auth.LoginResponse;
import com.roadcrack.service.service.AuthService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryAuthService implements AuthService {

    private final Map<String, LoginResponse> userStore = new ConcurrentHashMap<>();

    public InMemoryAuthService() {
        long now = System.currentTimeMillis();
        userStore.put("admin", new LoginResponse(
            "mock-token-admin-" + now, "mock-refresh-admin", "Bearer", 7200,
            1L, "admin", "超级管理员", List.of("ROLE_ADMIN")
        ));
        userStore.put("roadadmin", new LoginResponse(
            "mock-token-roadadmin-" + now, "mock-refresh-roadadmin", "Bearer", 7200,
            2L, "roadadmin", "周道路", List.of("ROLE_ROAD_ADMIN")
        ));
        userStore.put("sanitadmin", new LoginResponse(
            "mock-token-sanitadmin-" + now, "mock-refresh-sanitadmin", "Bearer", 7200,
            3L, "sanitadmin", "吴环卫", List.of("ROLE_SANIT_ADMIN")
        ));
    }

    @Override
    public LoginResponse login(LoginRequest request, String ipAddress) {
        String username = request.username();
        LoginResponse existing = userStore.get(username);
        if (existing != null) {
            return existing;
        }
        long now = System.currentTimeMillis();
        return new LoginResponse(
            "mock-token-" + username + "-" + now,
            "mock-refresh-" + username,
            "Bearer", 7200, 1L, username, username,
            List.of("ROLE_ADMIN")
        );
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        long now = System.currentTimeMillis();
        return new LoginResponse(
            "mock-token-refreshed-" + now, "mock-refresh-new-" + now,
            "Bearer", 7200, 1L, "admin", "超级管理员", List.of("ROLE_ADMIN")
        );
    }

    @Override public void register(RegisterRequest request) {}
    @Override public void sendVerificationCode(String email, Integer type) {}
    @Override public void changePassword(Long userId, ChangePasswordRequest request) {}
    @Override public void resetPassword(ResetPasswordRequest request) {}
}