package com.roadcrack.service.service.impl;

import com.roadcrack.api.request.user.UserPageQuery;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.dao.entity.UserEntity;
import com.roadcrack.service.service.AuditLogService;
import com.roadcrack.service.service.DepartmentService;
import com.roadcrack.service.service.RoleService;
import com.roadcrack.service.service.UserService;
import com.roadcrack.dao.entity.DepartmentEntity;
import com.roadcrack.dao.entity.RoleEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryUserService implements UserService {

    private final List<UserEntity> users = new ArrayList<>();
    private final AtomicLong idGen = new AtomicLong(0);

    private final java.util.Map<Long, Long> userRoleMap = new java.util.concurrent.ConcurrentHashMap<>();

    private final RoleService roleService;
    private final DepartmentService departmentService;
    private final AuditLogService auditLogService;

    public InMemoryUserService(@Lazy RoleService roleService,
                               @Lazy DepartmentService departmentService,
                               @Lazy AuditLogService auditLogService) {
        this.roleService = roleService;
        this.departmentService = departmentService;
        this.auditLogService = auditLogService;
        seedUsers();
    }

    private void seedUsers() {
        addUser(1L, "admin", "Admin@2025", "SUPER_ADMIN", "13800138000", "admin@roadcrack.cn", 1L, 1L);
        addUser(2L, "roadadmin", "Road@2025", "ROAD_ADMIN", "13800138001", "zhou@roadcrack.cn", 1L, 2L);
        addUser(3L, "sanitadmin", "Sanit@2025", "SANIT_ADMIN", "13800138002", "wu@roadcrack.cn", 4L, 3L);
        addUser(4L, "zhang_inspect", "Inspect@2025", "INSPECTOR_ZHANG", "13800138003", "zhang@roadcrack.cn", 2L, 4L);
        addUser(5L, "li_inspect", "Inspect@2025", "INSPECTOR_LI", "13800138004", "li@roadcrack.cn", 3L, 4L);
        addUser(6L, "wang_fix", "Fix@2025", "WORKER_WANG", "13800138005", "wang@roadcrack.cn", 5L, 5L);
        addUser(7L, "zhao_fix", "Fix@2025", "WORKER_ZHAO", "13800138006", "zhao@roadcrack.cn", 5L, 5L);
        addUser(8L, "sun_view", "View@2025", "VIEWER_SUN", "13800138007", "sun@roadcrack.cn", 1L, 6L);
        addUser(9L, "chen_admin", "Admin@2025", "ADMIN_CHEN", "13800138008", "chen@roadcrack.cn", 1L, 2L);
        addUser(10L, "liu_inspect", "Inspect@2025", "INSPECTOR_LIU", "13800138009", "liu@roadcrack.cn", 2L, 4L);
    }

    private void addUser(Long id, String username, String password, String realName,
                         String phone, String email, Long deptId, Long roleId) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setUsername(username);
        u.setPassword(password);
        u.setRealName(realName);
        u.setPhone(phone);
        u.setEmail(email);
        u.setDeptId(deptId);
        u.setStatus(1);
        u.setLoginFailCount(0);
        u.setLastLoginTime(LocalDateTime.now().minusHours(id * 3));
        u.setLastLoginIp("127.0.0.1");
        u.setDeleted(0);
        u.setCreateTime(LocalDateTime.of(2025, 1, 1, 9, 0).plusMinutes(id));
        u.setUpdateTime(LocalDateTime.of(2025, 1, 1, 9, 0).plusMinutes(id));
        users.add(u);
        userRoleMap.put(id, roleId);
        if (idGen.get() < id) idGen.set(id);
    }

    private UserEntity enrich(UserEntity u) {
        if (u == null) return null;
        Long roleId = userRoleMap.get(u.getId());
        if (roleId != null) {
            u.setRoleId(roleId);
            RoleEntity role = roleService.getById(roleId);
            if (role != null) {
                u.setRoleCode(role.getCode());
                u.setRoleName(role.getName());
            }
        }
        if (u.getDeptId() != null) {
            DepartmentEntity dept = departmentService.getById(u.getDeptId());
            if (dept != null) {
                u.setDeptName(dept.getName());
            }
        }
        return u;
    }

    @Override
    public PageResponse<UserEntity> pageQuery(UserPageQuery query) {
        List<UserEntity> filtered = new ArrayList<>(users);
        if (query.getUsername() != null && !query.getUsername().isBlank()) {
            filtered = filtered.stream()
                .filter(u -> u.getUsername().toLowerCase().contains(query.getUsername().toLowerCase()))
                .collect(Collectors.toList());
        }
        int total = filtered.size();
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getSize() == null || query.getSize() < 1 ? 10 : query.getSize();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<UserEntity> pageList = start >= total ? Collections.emptyList() : new ArrayList<>(filtered.subList(start, end));
        return new PageResponse<>(pageList, total, size, page, (long) Math.ceil((double) total / size));
    }

    @Override
    public UserEntity getById(Long id) {
        return enrich(users.stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null));
    }

    @Override
    public UserEntity findByUsername(String username) {
        return users.stream()
            .filter(u -> u.getUsername().equalsIgnoreCase(username))
            .findFirst()
            .orElse(null);
    }

    @Override
    public void createUser(UserEntity user, List<Long> roleIds) {
        if (users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(user.getUsername()))) {
            throw new BusinessException(ResultCode.USER_EXISTS, "username already exists: " + user.getUsername());
        }
        user.setId(idGen.incrementAndGet());
        if (user.getStatus() == null) user.setStatus(1);
        user.setLoginFailCount(0);
        user.setDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword("Default@2025");
        }
        users.add(user);
        if (roleIds != null && !roleIds.isEmpty()) {
            userRoleMap.put(user.getId(), roleIds.get(0));
        }
        // audit log skipped (compatibility)
    }

    @Override
    public void updateUser(UserEntity user, List<Long> roleIds) {
        UserEntity existing = requireUser(user.getId());
        if (user.getRealName() != null) existing.setRealName(user.getRealName());
        if (user.getPhone() != null) existing.setPhone(user.getPhone());
        if (user.getEmail() != null) existing.setEmail(user.getEmail());
        if (user.getDeptId() != null) existing.setDeptId(user.getDeptId());
        if (user.getStatus() != null) existing.setStatus(user.getStatus());
        if (user.getAvatar() != null) existing.setAvatar(user.getAvatar());
        existing.setUpdateTime(LocalDateTime.now());
        if (roleIds != null && !roleIds.isEmpty()) {
            userRoleMap.put(user.getId(), roleIds.get(0));
        }
        // audit log skipped (compatibility)
    }

    @Override
    public void deleteUser(Long id) {
        UserEntity user = requireUser(id);
        users.removeIf(u -> u.getId().equals(id));
        userRoleMap.remove(id);
        // audit log skipped (compatibility)
    }

    @Override
    public void toggleStatus(Long id, Integer status) {
        UserEntity user = requireUser(id);
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        // audit log skipped (compatibility)
    }

    @Override
    public void resetUserPassword(Long id) {
        UserEntity user = requireUser(id);
        user.setPassword("Reset@2025");
        user.setUpdateTime(LocalDateTime.now());
        // audit log skipped (compatibility)
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        Long roleId = userRoleMap.get(userId);
        return roleId != null ? List.of(roleId) : Collections.emptyList();
    }

    @Override
    public List<String> getUserRoleCodes(Long userId) {
        Long roleId = userRoleMap.get(userId);
        if (roleId == null) return Collections.emptyList();
        RoleEntity role = roleService.getById(roleId);
        return role != null ? List.of(role.getCode()) : Collections.emptyList();
    }

    private UserEntity requireUser(Long id) {
        Optional<UserEntity> opt = users.stream().filter(u -> u.getId().equals(id)).findFirst();
        if (opt.isEmpty()) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "user not found: " + id);
        }
        return opt.get();
    }
}
