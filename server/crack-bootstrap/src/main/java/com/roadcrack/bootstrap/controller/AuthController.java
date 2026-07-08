package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.request.auth.ChangePasswordRequest;
import com.roadcrack.api.request.auth.LoginRequest;
import com.roadcrack.api.request.auth.RegisterRequest;
import com.roadcrack.api.request.auth.ResetPasswordRequest;
import com.roadcrack.api.response.auth.LoginResponse;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.service.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success(authService.login(request, getClientIp(httpRequest)));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@RequestParam String refreshToken) {
        return ApiResponse.success(authService.refreshToken(refreshToken));
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/send-code")
    public ApiResponse<String> sendCode(@RequestParam String email,
                                        @RequestParam(defaultValue = "1") Integer type) {
        authService.sendVerificationCode(email, type);
        return ApiResponse.success("verification code accepted", null);
    }

    @PutMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestAttribute Long userId,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success(null);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
