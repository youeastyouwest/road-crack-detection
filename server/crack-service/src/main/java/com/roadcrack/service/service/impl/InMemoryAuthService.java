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
import com.roadcrack.service.service.SystemConfigService;
import com.roadcrack.service.service.UserService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryAuthService implements AuthService {

    private static final int REGISTER_CODE_TYPE = 1;
    private static final int RESET_PASSWORD_CODE_TYPE = 2;
    private static final int CODE_EXPIRE_MINUTES = 5;
    private static final long DEFAULT_ROLE_ID = 6L; // VIEWER role for new users

    private final UserService userService;
    private final SystemConfigService systemConfigService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    // In-memory verification code store: key = "email:type", value = code info
    private final ConcurrentHashMap<String, CodeEntry> codeStore = new ConcurrentHashMap<>();

    public InMemoryAuthService(UserService userService,
                               SystemConfigService systemConfigService,
                               ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.userService = userService;
        this.systemConfigService = systemConfigService;
        this.mailSenderProvider = mailSenderProvider;
    }

    // === Verification Code ===

    @Override
    public void sendVerificationCode(String email, Integer type) {
        int codeType = type == null ? REGISTER_CODE_TYPE : type;
        String key = email + ":" + codeType;

        // If a valid unused code already exists, do not regenerate or resend.
        CodeEntry existing = codeStore.get(key);
        if (existing != null && !existing.used
                && existing.expireTime != null
                && !existing.expireTime.isBefore(LocalDateTime.now())) {
            System.out.println("[MAIL] 验证码已存在且未过期，拒绝重复发送 email=" + email + " type=" + codeType);
            return;
        }

        // Remove expired code if any
        if (existing != null && existing.expireTime != null
                && existing.expireTime.isBefore(LocalDateTime.now())) {
            codeStore.remove(key);
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));

        CodeEntry entry = new CodeEntry();
        entry.code = code;
        entry.type = codeType;
        entry.used = false;
        entry.expireTime = LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES);

        codeStore.put(key, entry);

        // Try to send real email
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender != null && mailFrom != null && !mailFrom.isBlank()) {
            try {
                String subject = codeType == REGISTER_CODE_TYPE
                        ? "【道路裂缝检测系统】注册验证码"
                        : "【道路裂缝检测系统】重置密码验证码";
                String body = buildEmailBody(code, codeType);
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setFrom(mailFrom);
                helper.setTo(email);
                helper.setSubject(subject);
                helper.setText(body, true);
                mailSender.send(mimeMessage);
                System.out.println("[MAIL] 验证码已发送至 " + email + " code=" + code);
            } catch (Exception e) {
                // Rollback the code store on send failure so user can retry
                codeStore.remove(key);
                System.err.println("[MAIL ERROR] 邮件发送失败: " + e.getMessage());
                throw new BusinessException(ResultCode.VERIFICATION_CODE_SEND_FAIL, "验证码邮件发送失败，请稍后重试");
            }
        } else {
            // Fallback: log to console when mail is not configured
            System.out.println("[VERIFICATION CODE] (邮件未配置) email=" + email + " type=" + codeType + " code=" + code);
        }
    }

    /**
     * Check if real email sending is configured.
     */
    public boolean isMailConfigured() {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        return mailSender != null && mailFrom != null && !mailFrom.isBlank();
    }

    private String buildEmailBody(String code, int type) {
        String action = type == REGISTER_CODE_TYPE ? "注册" : "重置密码";
        return "<div style='font-family:Arial,sans-serif;max-width:500px;margin:0 auto;border:1px solid #e0e0e0;border-radius:8px;overflow:hidden;'>"
                + "<div style='background:#409EFF;color:#fff;padding:20px;text-align:center;'>"
                + "<h2 style='margin:0;'>道路裂缝检测系统</h2>"
                + "</div>"
                + "<div style='padding:30px;'>"
                + "<p style='font-size:16px;color:#333;'>您正在" + action + "账号，验证码如下：</p>"
                + "<div style='background:#f5f7fa;border-radius:6px;padding:20px;text-align:center;margin:20px 0;'>"
                + "<span style='font-size:32px;font-weight:bold;color:#409EFF;letter-spacing:6px;'>" + code + "</span>"
                + "</div>"
                + "<p style='font-size:13px;color:#999;'>验证码 " + CODE_EXPIRE_MINUTES + " 分钟内有效，请勿泄露给他人。</p>"
                + "<p style='font-size:13px;color:#999;'>如非本人操作，请忽略此邮件。</p>"
                + "</div>"
                + "<div style='background:#f5f7fa;color:#999;padding:15px;text-align:center;font-size:12px;'>"
                + "此邮件由系统自动发送，请勿回复。"
                + "</div>"
                + "</div>";
    }

    /**
     * Get the latest verification code for a given email and type (for response).
     */
    public String getLatestCode(String email, Integer type) {
        int codeType = type == null ? REGISTER_CODE_TYPE : type;
        String key = email + ":" + codeType;
        CodeEntry entry = codeStore.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expireTime != null && entry.expireTime.isBefore(LocalDateTime.now())) {
            codeStore.remove(key);
            return null;
        }
        if (entry.used) {
            return null;
        }
        return entry.code;
    }

    // === Register ===

    @Override
    public void register(RegisterRequest request) {
        if (!systemConfigService.isRegisterEnabled()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前系统已关闭自助注册功能");
        }

        int minPasswordLength = systemConfigService.getConfig().getMinPasswordLength();
        if (request.password() == null || request.password().length() < minPasswordLength) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码长度不能少于 " + minPasswordLength + " 位");
        }

        validateCode(request.email(), request.code(), REGISTER_CODE_TYPE);

        InMemoryUserService memUserService = (InMemoryUserService) userService;

        // Check username duplicate
        if (memUserService.findByUsername(request.username()) != null) {
            throw new BusinessException(ResultCode.USER_EXISTS, "用户名已存在: " + request.username());
        }

        // Check email duplicate
        if (findByEmail(request.email()) != null) {
            throw new BusinessException(ResultCode.EMAIL_EXISTS, "邮箱已被注册: " + request.email());
        }

        // Create user entity
        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setEmail(request.email());
        user.setRealName(request.realName() != null ? request.realName() : request.username());
        user.setPhone(request.phone() != null ? request.phone() : "");
        user.setDeptId(request.deptId() != null ? request.deptId() : 1L);
        user.setStatus(1);
        user.setLoginFailCount(0);
        user.setDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        Long selectedRoleId = request.roleId() != null ? request.roleId() : DEFAULT_ROLE_ID;
        memUserService.createUser(user, List.of(selectedRoleId));
    }

    // === Change Password ===

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        InMemoryUserService memUserService = (InMemoryUserService) userService;
        UserEntity user = memUserService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "用户不存在");
        }
        if (!request.oldPassword().equals(user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR, "原密码错误");
        }
        user.setPassword(request.newPassword());
        user.setUpdateTime(LocalDateTime.now());
        memUserService.updateUser(user, null);
    }

    // === Reset Password ===

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        validateCode(request.email(), request.code(), RESET_PASSWORD_CODE_TYPE);

        UserEntity user = findByEmail(request.email());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "该邮箱未注册");
        }

        user.setPassword(request.newPassword());
        user.setStatus(1);
        user.setLoginFailCount(0);
        user.setUpdateTime(LocalDateTime.now());
        ((InMemoryUserService) userService).updateUser(user, null);
    }

    // === Login ===

    @Override
    public LoginResponse login(LoginRequest request, String ipAddress) {
        String username = request.username();
        String password = request.password();

        InMemoryUserService memUserService = (InMemoryUserService) userService;
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

    // === Private helpers ===

    private void validateCode(String email, String code, Integer type) {
        String key = email + ":" + type;
        CodeEntry entry = codeStore.get(key);
        if (entry == null) {
            throw new BusinessException(ResultCode.VERIFICATION_CODE_ERROR, "验证码无效或已过期");
        }
        if (entry.used) {
            throw new BusinessException(ResultCode.VERIFICATION_CODE_ERROR, "验证码已被使用");
        }
        if (entry.expireTime != null && entry.expireTime.isBefore(LocalDateTime.now())) {
            codeStore.remove(key);
            throw new BusinessException(ResultCode.VERIFICATION_CODE_ERROR, "验证码已过期");
        }
        if (!entry.code.equals(code)) {
            throw new BusinessException(ResultCode.VERIFICATION_CODE_ERROR, "验证码错误");
        }
        // Mark as used
        entry.used = true;
    }

    private UserEntity findByEmail(String email) {
        InMemoryUserService memUserService = (InMemoryUserService) userService;
        return memUserService.findByEmail(email);
    }

    // === Inner class ===

    private static class CodeEntry {
        String code;
        int type;
        boolean used;
        LocalDateTime expireTime;
    }
}
