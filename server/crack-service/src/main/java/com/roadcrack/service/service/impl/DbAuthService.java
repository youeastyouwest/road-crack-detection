package com.roadcrack.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.roadcrack.api.request.auth.ChangePasswordRequest;
import com.roadcrack.api.request.auth.LoginRequest;
import com.roadcrack.api.request.auth.RegisterRequest;
import com.roadcrack.api.request.auth.ResetPasswordRequest;
import com.roadcrack.api.response.auth.LoginResponse;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.dao.entity.RoleEntity;
import com.roadcrack.dao.entity.UserEntity;
import com.roadcrack.dao.entity.UserRoleEntity;
import com.roadcrack.dao.entity.VerificationCodeEntity;
import com.roadcrack.dao.mapper.RoleMapper;
import com.roadcrack.dao.mapper.UserMapper;
import com.roadcrack.dao.mapper.UserRoleMapper;
import com.roadcrack.dao.mapper.VerificationCodeMapper;
import com.roadcrack.service.security.JwtUtils;
import com.roadcrack.service.service.AuthService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbAuthService implements AuthService {

    private static final int REGISTER_CODE_TYPE = 1;
    private static final int RESET_PASSWORD_CODE_TYPE = 2;

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final VerificationCodeMapper verificationCodeMapper;
    private final RoleMapper roleMapper;
    private final JwtUtils jwtUtils;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final String mailFrom;
    private final int maxFailCount;
    private final int lockDurationMinutes;

    public DbAuthService(UserMapper userMapper,
                         UserRoleMapper userRoleMapper,
                         VerificationCodeMapper verificationCodeMapper,
                         RoleMapper roleMapper,
                         JwtUtils jwtUtils,
                         ObjectProvider<JavaMailSender> mailSenderProvider,
                         @Value("${spring.mail.username:}") String mailFrom,
                         @Value("${security.login.max-fail-count:5}") int maxFailCount,
                         @Value("${security.login.lock-duration:15}") int lockDurationMinutes) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.verificationCodeMapper = verificationCodeMapper;
        this.roleMapper = roleMapper;
        this.jwtUtils = jwtUtils;
        this.mailSenderProvider = mailSenderProvider;
        this.mailFrom = mailFrom;
        this.maxFailCount = maxFailCount;
        this.lockDurationMinutes = lockDurationMinutes;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, request.username())
                .last("limit 1"));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR, "invalid username or password");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_DISABLED, "user is disabled");
        }
        if (Integer.valueOf(2).equals(user.getStatus())) {
            if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
                throw new BusinessException(ResultCode.USER_LOCKED, "user is locked");
            }
            user.setStatus(1);
            user.setLoginFailCount(0);
            user.setLockUntil(null);
            userMapper.updateById(user);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            int failCount = user.getLoginFailCount() == null ? 0 : user.getLoginFailCount();
            user.setLoginFailCount(failCount + 1);
            if (user.getLoginFailCount() >= maxFailCount) {
                user.setStatus(2);
                user.setLockUntil(LocalDateTime.now().plusMinutes(lockDurationMinutes));
            }
            userMapper.updateById(user);
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR, "invalid username or password");
        }

        user.setStatus(1);
        user.setLoginFailCount(0);
        user.setLockUntil(null);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        userMapper.updateById(user);

        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtUtils.validateToken(refreshToken) || !jwtUtils.isRefreshToken(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "invalid refresh token");
        }
        Long userId = jwtUtils.getUserId(refreshToken);
        UserEntity user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "user not available");
        }
        return buildLoginResponse(user);
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        validateCode(request.email(), request.code(), REGISTER_CODE_TYPE);

        if (countByUsername(request.username(), null) > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS, "username already exists");
        }
        if (countByEmail(request.email(), null) > 0) {
            throw new BusinessException(ResultCode.EMAIL_EXISTS, "email already exists");
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setRealName(hasText(request.realName()) ? request.realName() : request.username());
        user.setPhone(request.phone());
        user.setDeptId(request.deptId());
        user.setStatus(1);
        user.setLoginFailCount(0);
        userMapper.insert(user);

        RoleEntity defaultRole = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getCode, "ROLE_CROWDSOURCE")
                .last("limit 1"));
        if (defaultRole != null) {
            UserRoleEntity relation = new UserRoleEntity();
            relation.setUserId(user.getId());
            relation.setRoleId(defaultRole.getId());
            relation.setCreateTime(LocalDateTime.now());
            userRoleMapper.insert(relation);
        }
    }

    @Override
    @Transactional
    public void sendVerificationCode(String email, Integer type) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        VerificationCodeEntity entity = new VerificationCodeEntity();
        entity.setEmail(email);
        entity.setCode(code);
        entity.setType(type == null ? REGISTER_CODE_TYPE : type);
        entity.setUsed(0);
        entity.setExpireTime(LocalDateTime.now().plusMinutes(5));
        verificationCodeMapper.insert(entity);

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || !hasText(mailFrom)) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject(type != null && type == RESET_PASSWORD_CODE_TYPE ? "Reset password code" : "Register verification code");
            helper.setText("Your verification code is " + code + ". It expires in 5 minutes.", false);
            mailSender.send(message);
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.VERIFICATION_CODE_SEND_FAIL, "failed to send verification code");
        }
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "user not found");
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR, "old password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        validateCode(request.email(), request.code(), RESET_PASSWORD_CODE_TYPE);
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getEmail, request.email())
                .last("limit 1"));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "user not found");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setStatus(1);
        user.setLoginFailCount(0);
        user.setLockUntil(null);
        userMapper.updateById(user);
    }

    private LoginResponse buildLoginResponse(UserEntity user) {
        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        return new LoginResponse(
                jwtUtils.generateToken(user.getId(), user.getUsername()),
                jwtUtils.generateRefreshToken(user.getId(), user.getUsername()),
                "Bearer",
                86400,
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                roles
        );
    }

    private void validateCode(String email, String code, Integer type) {
        VerificationCodeEntity entity = verificationCodeMapper.selectOne(new LambdaQueryWrapper<VerificationCodeEntity>()
                .eq(VerificationCodeEntity::getEmail, email)
                .eq(VerificationCodeEntity::getCode, code)
                .eq(VerificationCodeEntity::getType, type)
                .eq(VerificationCodeEntity::getUsed, 0)
                .orderByDesc(VerificationCodeEntity::getCreateTime)
                .last("limit 1"));
        if (entity == null || entity.getExpireTime() == null || entity.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.VERIFICATION_CODE_ERROR, "verification code is invalid or expired");
        }
        entity.setUsed(1);
        verificationCodeMapper.updateById(entity);
    }

    private long countByUsername(String username, Long excludeId) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(UserEntity::getId, excludeId);
        }
        return userMapper.selectCount(wrapper);
    }

    private long countByEmail(String email, Long excludeId) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getEmail, email);
        if (excludeId != null) {
            wrapper.ne(UserEntity::getId, excludeId);
        }
        return userMapper.selectCount(wrapper);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
