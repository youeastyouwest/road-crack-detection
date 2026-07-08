package com.roadcrack.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.dao.entity.RoleEntity;
import com.roadcrack.dao.entity.UserRoleEntity;
import com.roadcrack.dao.mapper.RoleMapper;
import com.roadcrack.dao.mapper.UserRoleMapper;
import com.roadcrack.service.service.RoleService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbRoleService implements RoleService {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public DbRoleService(RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public List<RoleEntity> listEnabledRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getStatus, 1)
                .orderByAsc(RoleEntity::getId));
    }

    @Override
    public RoleEntity getById(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    public void createRole(RoleEntity role) {
        if (countByCode(role.getCode(), null) > 0) {
            throw new BusinessException(ResultCode.ROLE_CODE_EXISTS, "role code already exists");
        }
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        roleMapper.insert(role);
    }

    @Override
    public void updateRole(RoleEntity role) {
        requireRole(role.getId());
        if (countByCode(role.getCode(), role.getId()) > 0) {
            throw new BusinessException(ResultCode.ROLE_CODE_EXISTS, "role code already exists");
        }
        roleMapper.updateById(role);
    }

    @Override
    public void deleteRole(Long id) {
        RoleEntity role = requireRole(id);
        long userCount = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getRoleId, id));
        if (userCount > 0) {
            throw new BusinessException(ResultCode.ROLE_HAS_USERS, "role is still assigned to users");
        }
        roleMapper.deleteById(role.getId());
    }

    private RoleEntity requireRole(Long id) {
        RoleEntity role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND, "role not found: " + id);
        }
        return role;
    }

    private long countByCode(String code, Long excludeId) {
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getCode, code);
        if (excludeId != null) {
            wrapper.ne(RoleEntity::getId, excludeId);
        }
        return roleMapper.selectCount(wrapper);
    }
}
