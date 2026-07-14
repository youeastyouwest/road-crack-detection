package com.roadcrack.service.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadcrack.api.request.user.UserPageQuery;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.dao.entity.DepartmentEntity;
import com.roadcrack.dao.entity.RoleEntity;
import com.roadcrack.dao.entity.UserEntity;
import com.roadcrack.dao.entity.UserRoleEntity;
import com.roadcrack.dao.mapper.DepartmentMapper;
import com.roadcrack.dao.mapper.RoleMapper;
import com.roadcrack.dao.mapper.UserMapper;
import com.roadcrack.dao.mapper.UserRoleMapper;
import com.roadcrack.service.service.AuditLogService;
import com.roadcrack.service.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbUserService implements UserService {

    private static final String DEFAULT_PASSWORD = "123456";

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final DepartmentMapper departmentMapper;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DbUserService(UserMapper userMapper,
                         UserRoleMapper userRoleMapper,
                         RoleMapper roleMapper,
                         DepartmentMapper departmentMapper,
                         AuditLogService auditLogService) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.departmentMapper = departmentMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    public PageResponse<UserEntity> pageQuery(UserPageQuery query) {
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getSize() == null || query.getSize() < 1 ? 10 : query.getSize();

        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>()
                .orderByDesc(UserEntity::getCreateTime);
        if (hasText(query.getUsername())) {
            wrapper.like(UserEntity::getUsername, query.getUsername());
        }
        if (hasText(query.getRealName())) {
            wrapper.like(UserEntity::getRealName, query.getRealName());
        }
        if (hasText(query.getPhone())) {
            wrapper.eq(UserEntity::getPhone, query.getPhone());
        }
        if (query.getStatus() != null) {
            wrapper.eq(UserEntity::getStatus, query.getStatus());
        }
        if (query.getDeptId() != null) {
            wrapper.eq(UserEntity::getDeptId, query.getDeptId());
        }

        Page<UserEntity> result = userMapper.selectPage(new Page<>(page, size), wrapper);

        for (UserEntity user : result.getRecords()) {
            enrich(user);
        }

        return new PageResponse<>(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent(), result.getPages());
    }

    @Override
    public UserEntity getById(Long id) {
        UserEntity user = userMapper.selectById(id);
        if (user != null) {
            enrich(user);
        }
        return user;
    }

    @Override
    @Transactional
    public void createUser(UserEntity user, List<Long> roleIds) {
        if (countByUsername(user.getUsername(), null) > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS, "username already exists");
        }

        if (hasText(user.getEmail()) && countByEmail(user.getEmail(), null) > 0) {
            throw new BusinessException(ResultCode.EMAIL_EXISTS, "email already exists");
        }

        String rawPassword = hasText(user.getPassword()) ? user.getPassword() : DEFAULT_PASSWORD;
        user.setPassword(passwordEncoder.encode(rawPassword));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        if (user.getLoginFailCount() == null) {
            user.setLoginFailCount(0);
        }
        userMapper.insert(user);
        replaceRoles(user.getId(), roleIds);

        enrich(user);
        auditLogService.record(
                user.getUsername(), "USER", "CREATE",
                "创建用户: " + user.getUsername() + " (" + (user.getRealName() != null ? user.getRealName() : "") + ")",
                "", 0L, "SUCCESS", ""
        );
    }

    @Override
    @Transactional
    public void updateUser(UserEntity user, List<Long> roleIds) {
        UserEntity existing = requireUser(user.getId());

        if (hasText(user.getUsername()) && countByUsername(user.getUsername(), user.getId()) > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS, "username already exists");
        }

        if (hasText(user.getEmail()) && countByEmail(user.getEmail(), user.getId()) > 0) {
            throw new BusinessException(ResultCode.EMAIL_EXISTS, "email already exists");
        }

        user.setPassword(existing.getPassword());
        user.setLoginFailCount(existing.getLoginFailCount());
        user.setLockUntil(existing.getLockUntil());
        user.setLastLoginTime(existing.getLastLoginTime());
        user.setLastLoginIp(existing.getLastLoginIp());
        userMapper.updateById(user);

        if (roleIds != null) {
            replaceRoles(user.getId(), roleIds);
        }

        enrich(user);
        auditLogService.record(
                user.getUsername(), "USER", "UPDATE",
                "更新用户: " + user.getUsername() + " (" + (user.getRealName() != null ? user.getRealName() : "") + ")",
                "", 0L, "SUCCESS", ""
        );
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        UserEntity user = requireUser(id);
        String username = user.getUsername();
        String realName = user.getRealName();
        String email = user.getEmail();

        // Release unique fields before logical deletion so the same account can be recreated later.
        user.setUsername(buildDeletedValue(username, id, 64));
        if (hasText(email)) {
            user.setEmail(buildDeletedValue(email, id, 128));
        }
        userMapper.updateById(user);

        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, id));
        userMapper.deleteById(id);

        auditLogService.record(
                username, "USER", "DELETE",
                "删除用户: " + username + " (" + realName + ")",
                "", 0L, "SUCCESS", ""
        );
    }

    @Override
    public void toggleStatus(Long id, Integer status) {
        UserEntity user = requireUser(id);
        user.setStatus(status);
        if (Integer.valueOf(1).equals(status)) {
            user.setLoginFailCount(0);
            user.setLockUntil(null);
        }
        userMapper.updateById(user);

        String action = Integer.valueOf(1).equals(status) ? "ENABLE" : "DISABLE";
        String desc = Integer.valueOf(1).equals(status) ? "启用用户" : "禁用用户";
        auditLogService.record(
                user.getUsername(), "USER", action,
                desc + ": " + user.getUsername(),
                "", 0L, "SUCCESS", ""
        );
    }

    @Override
    public void resetUserPassword(Long id) {
        UserEntity user = requireUser(id);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setStatus(1);
        user.setLoginFailCount(0);
        user.setLockUntil(null);
        userMapper.updateById(user);

        auditLogService.record(
                user.getUsername(), "USER", "RESET_PASSWORD",
                "重置用户密码: " + user.getUsername(),
                "", 0L, "SUCCESS", ""
        );
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        requireUser(userId);
        return userMapper.selectRoleIdsByUserId(userId);
    }

    @Override
    public UserEntity findByUsername(String username) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username)
                .last("LIMIT 1");
        UserEntity user = userMapper.selectOne(wrapper);
        if (user != null) enrich(user);
        return user;
    }

    @Override
    public List<String> getUserRoleCodes(Long userId) {
        requireUser(userId);
        return userMapper.selectRoleCodesByUserId(userId);
    }

    private void enrich(UserEntity user) {
        if (user == null) return;
        user.setPassword(null);

        List<Long> roleIds = userMapper.selectRoleIdsByUserId(user.getId());
        List<String> roleCodes = userMapper.selectRoleCodesByUserId(user.getId());

        if (roleIds != null && !roleIds.isEmpty()) {
            user.setRoleId(roleIds.get(0));
            RoleEntity role = roleMapper.selectById(roleIds.get(0));
            if (role != null) {
                user.setRoleCode(role.getCode());
                user.setRoleName(role.getName());
            }
        }
        if (roleCodes != null && !roleCodes.isEmpty()) {
            user.setRoleCode(roleCodes.get(0));
        }

        if (user.getDeptId() != null) {
            DepartmentEntity dept = departmentMapper.selectById(user.getDeptId());
            if (dept != null) {
                user.setDeptName(dept.getName());
            }
        }
    }

    private UserEntity requireUser(Long id) {
        UserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "user not found: " + id);
        }
        return user;
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, userId));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            UserRoleEntity relation = new UserRoleEntity();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            relation.setCreateTime(LocalDateTime.now());
            userRoleMapper.insert(relation);
        }
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

    private String buildDeletedValue(String originalValue, Long id, int maxLength) {
        String suffix = "_deleted_" + id + "_" + System.currentTimeMillis();
        if (!hasText(originalValue)) {
            return suffix.length() <= maxLength ? suffix : suffix.substring(0, maxLength);
        }
        int prefixLength = Math.max(0, maxLength - suffix.length());
        String prefix = originalValue.length() > prefixLength ? originalValue.substring(0, prefixLength) : originalValue;
        return prefix + suffix;
    }
}
