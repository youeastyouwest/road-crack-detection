package com.roadcrack.api.response.auth;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String username,
        String realName,
        List<String> roles
) {
}
