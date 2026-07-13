package com.roadcrack.service.service.impl;

import com.roadcrack.api.request.auth.ChangePasswordRequest;
import com.roadcrack.api.request.auth.LoginRequest;
import com.roadcrack.api.request.auth.RegisterRequest;
import com.roadcrack.api.request.auth.ResetPasswordRequest;
import com.roadcrack.api.response.auth.LoginResponse;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.dao.entity.UserEntity;
import com.roadcrack.service.service.AuthService;
import com.roadcrack.service.service.UserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryAuthService implements AuthService {

    private final UserService userService;

    public InMemoryAuthService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public LoginResponse login(LoginRequest request, String ipAddress) {
        String username = request.username();
        String password = request.password();

        // Validate credentials against InMemoryUserService
        com.roadcrack.service.service.impl.InMemoryUserService memUserService =
            (com.roadcrack.service.service.impl.InMemoryUserService) userService;
        UserEntity user = memUserService.findByUsername(username);

        if (user == null) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_DISABLED, "账号已被禁用");
        }
        if (!password.equals(user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR, "用户名或密码错误");
        }

        // Update last login
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);

        List<String> roleCodes = userService.getUserRoleCodes(user.getId());
        if (roleCodes.isEmpty()) {
            roleCodes = List.of("ROLE_ADMIN");
        }

        long now = System.currentTimeMillis();
        return new LoginResponse(
            "mock-token-" + username + "-" + now,
            "mock-refresh-" + username + "-" + now,
            "Bearer", 7200,
            user.getId(), user.getUsername(), user.getRealName() != null ? user.getRealName() : username,
            roleCodes
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
