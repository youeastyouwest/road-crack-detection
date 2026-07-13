package com.roadcrack.service.service.impl;

import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.dao.entity.RoleEntity;
import com.roadcrack.service.service.RoleService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryRoleService implements RoleService {

    private final List<RoleEntity> roles = new ArrayList<>();
    private final AtomicLong idGen = new AtomicLong(0);

    public InMemoryRoleService() {
        seedRoles();
    }

    private void seedRoles() {
        addRole(1L, "系统管理员", "ROLE_ADMIN", "拥有系统全部管理权限，可管理用户、部门、角色、系统配置", 1);
        addRole(2L, "道路管理员", "ROLE_ROAD_ADMIN", "负责道路巡检工单管理、检测任务查看、工单指派", 1);
        addRole(3L, "环卫管理员", "ROLE_SANIT_ADMIN", "负责环卫相关工单管理和道路保洁任务分配", 1);
        addRole(4L, "巡检员", "ROLE_INSPECTOR", "负责道路巡查、上传检测图片、查看检测结果", 1);
        addRole(5L, "维修工", "ROLE_MAINTAINER", "负责接收维修工单、执行维修任务、提交维修报告", 1);
        addRole(6L, "查看员", "ROLE_VIEWER", "只读权限，可查看数据大屏和检测报告", 1);
    }

    private void addRole(Long id, String name, String code, String desc, int status) {
        RoleEntity r = new RoleEntity();
        r.setId(id);
        r.setName(name);
        r.setCode(code);
        r.setDescription(desc);
        r.setStatus(status);
        r.setCreateTime(LocalDateTime.of(2025, 1, 1, 9, 0));
        r.setUpdateTime(LocalDateTime.of(2025, 1, 1, 9, 0));
        roles.add(r);
        if (idGen.get() < id) idGen.set(id);
    }

    @Override
    public List<RoleEntity> listEnabledRoles() {
        return new ArrayList<>(roles);
    }

    @Override
    public RoleEntity getById(Long id) {
        return roles.stream().filter(r -> r.getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public void createRole(RoleEntity role) {
        // 检查编码唯一性
        if (roles.stream().anyMatch(r -> r.getCode().equalsIgnoreCase(role.getCode()))) {
            throw new BusinessException(ResultCode.ROLE_CODE_EXISTS, "角色编码已存在: " + role.getCode());
        }
        role.setId(idGen.incrementAndGet());
        if (role.getStatus() == null) role.setStatus(1);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roles.add(role);
    }

    @Override
    public void updateRole(RoleEntity role) {
        RoleEntity existing = requireRole(role.getId());
        // 检查编码唯一性（排除自身）
        if (roles.stream().anyMatch(r -> !r.getId().equals(role.getId()) && r.getCode().equalsIgnoreCase(role.getCode()))) {
            throw new BusinessException(ResultCode.ROLE_CODE_EXISTS, "角色编码已存在: " + role.getCode());
        }
        if (role.getName() != null) existing.setName(role.getName());
        if (role.getCode() != null) existing.setCode(role.getCode());
        if (role.getDescription() != null) existing.setDescription(role.getDescription());
        if (role.getStatus() != null) existing.setStatus(role.getStatus());
        existing.setUpdateTime(LocalDateTime.now());
    }

    @Override
    public void deleteRole(Long id) {
        RoleEntity role = requireRole(id);
        // 内置角色不可删除
        if (role.getId() <= 6L) {
            throw new BusinessException(ResultCode.ROLE_HAS_USERS, "内置角色不可删除");
        }
        roles.removeIf(r -> r.getId().equals(id));
    }

    private RoleEntity requireRole(Long id) {
        Optional<RoleEntity> opt = roles.stream().filter(r -> r.getId().equals(id)).findFirst();
        if (opt.isEmpty()) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND, "角色不存在: " + id);
        }
        return opt.get();
    }
}
