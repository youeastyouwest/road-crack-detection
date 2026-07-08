package com.roadcrack.service.service;

import com.roadcrack.api.request.auth.ChangePasswordRequest;
import com.roadcrack.api.request.auth.LoginRequest;
import com.roadcrack.api.request.auth.RegisterRequest;
import com.roadcrack.api.request.auth.ResetPasswordRequest;
import com.roadcrack.api.response.auth.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request, String ipAddress);

    LoginResponse refreshToken(String refreshToken);

    void register(RegisterRequest request);

    void sendVerificationCode(String email, Integer type);

    void changePassword(Long userId, ChangePasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
