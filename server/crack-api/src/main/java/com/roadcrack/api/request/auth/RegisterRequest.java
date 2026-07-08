package com.roadcrack.api.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "用户名长度必须在 3-32 位之间")
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度必须在 6-32 位之间")
        String password,
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email,
        String realName,
        String phone,
        @NotBlank(message = "验证码不能为空")
        String code,
        Long deptId
) {
}
