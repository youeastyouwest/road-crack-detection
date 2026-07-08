package com.roadcrack.api.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "邮箱不能为空") String email,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度必须在 6-32 位之间")
        String newPassword,
        @NotBlank(message = "验证码不能为空") String code
) {
}
